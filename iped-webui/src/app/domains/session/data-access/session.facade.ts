import { Injectable, computed, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { CasesService } from '../../../core/api/generated/api/cases.service';
import { CaseMetadata } from '../../../core/api/generated/model/caseMetadata';
import { CaseSession } from '../../../core/api/generated/model/caseSession';

@Injectable({
  providedIn: 'root'
})
export class SessionFacade {
  readonly caseId = signal('');
  readonly metadata = signal<CaseMetadata | null>(null);
  readonly session = signal<CaseSession | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly hasActiveSession = computed(() => this.session() !== null);

  constructor(private readonly casesService: CasesService) {}

  async bootstrap(caseId: string, userId?: string) {
    const normalizedCaseId = caseId.trim();

    if (!normalizedCaseId) {
      this.error.set('Informe um caseId para abrir a sessão.');
      return false;
    }

    this.loading.set(true);
    this.error.set(null);

    try {
      const [metadata, session] = await Promise.all([
        firstValueFrom(this.casesService.getCaseMetadata({ caseId: normalizedCaseId })),
        firstValueFrom(
          this.casesService.createCaseSession({
            caseId: normalizedCaseId,
            caseSessionCreateRequest: {
              userId: userId?.trim() || undefined,
              clientInfo: {
                client: 'iped-webui',
                workflow: 'epic-web-02'
              }
            }
          })
        )
      ]);

      this.caseId.set(normalizedCaseId);
      this.metadata.set(metadata);
      this.session.set(session);

      return true;
    } catch (error) {
      this.metadata.set(null);
      this.session.set(null);
      this.error.set(this.toMessage(error, 'Falha ao abrir a sessão do caso.'));
      return false;
    } finally {
      this.loading.set(false);
    }
  }

  clear() {
    this.caseId.set('');
    this.metadata.set(null);
    this.session.set(null);
    this.error.set(null);
  }

  private toMessage(error: unknown, fallback: string) {
    if (error instanceof Error && error.message) {
      return error.message;
    }

    return fallback;
  }
}
