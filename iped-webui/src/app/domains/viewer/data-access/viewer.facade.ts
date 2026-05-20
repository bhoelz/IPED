import { Injectable, signal } from '@angular/core';
import { DomSanitizer, SafeHtml, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';

import { ItemsService } from '../../../core/api/generated/api/items.service';
import { ViewerService } from '../../../core/api/generated/api/viewer.service';
import { ItemDetails } from '../../../core/api/generated/model/itemDetails';
import {
  RenditionLink,
  RenditionLinkKindEnum
} from '../../../core/api/generated/model/renditionLink';
import {
  ViewerHitState
} from '../../../core/api/generated/model/viewerHitState';
import {
  ViewerNavigateHitRequestDirectionEnum
} from '../../../core/api/generated/model/viewerNavigateHitRequest';
import { ViewerOpenResponse } from '../../../core/api/generated/model/viewerOpenResponse';
import {
  ViewerSearchRequestMatchModeEnum
} from '../../../core/api/generated/model/viewerSearchRequest';

type ViewerRenderableKind = 'text' | 'html' | 'image' | 'pdf' | 'unsupported';

@Injectable({
  providedIn: 'root'
})
export class ViewerFacade {
  readonly session = signal<ViewerOpenResponse | null>(null);
  readonly kind = signal<ViewerRenderableKind>('unsupported');
  readonly textContent = signal<string | null>(null);
  readonly htmlContent = signal<SafeHtml | null>(null);
  readonly resourceUrl = signal<SafeResourceUrl | null>(null);
  readonly searchTerm = signal('');
  readonly hitState = signal<ViewerHitState | null>(null);
  readonly loading = signal(false);
  readonly searching = signal(false);
  readonly error = signal<string | null>(null);

  private objectUrl: string | null = null;

  constructor(
    private readonly viewerService: ViewerService,
    private readonly itemsService: ItemsService,
    private readonly sanitizer: DomSanitizer
  ) {}

  async open(
    caseId: string,
    item: ItemDetails,
    options?: {
      queryId?: string | null;
      selectedRow?: number;
    }
  ) {
    const normalizedCaseId = caseId.trim();
    const mimeType = item.mediaType?.trim();

    if (!normalizedCaseId || !item.itemId || !mimeType) {
      this.error.set('O item selecionado não possui dados suficientes para abrir o viewer.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.resetRenderedContent();

    try {
      const session = await firstValueFrom(
        this.viewerService.openViewerSession({
          caseId: normalizedCaseId,
          viewerOpenRequest: {
            itemId: item.itemId,
            mimeType,
            context: {
              queryId: options?.queryId ?? undefined,
              selectedRow: options?.selectedRow
            }
          }
        })
      );

      this.session.set(session);
      this.searchTerm.set('');
      this.hitState.set(null);

      const targetKind = this.chooseRenditionKind(session.renditions ?? [], mimeType);
      await this.loadRendition(normalizedCaseId, item.itemId, targetKind);
    } catch (error) {
      this.session.set(null);
      this.kind.set('unsupported');
      this.error.set(this.toMessage(error, 'Falha ao abrir a sessão do viewer.'));
    } finally {
      this.loading.set(false);
    }
  }

  clear() {
    this.session.set(null);
    this.error.set(null);
    this.kind.set('unsupported');
    this.searchTerm.set('');
    this.hitState.set(null);
    this.resetRenderedContent();
  }

  async search(caseId: string, term: string) {
    const session = this.session();
    const normalizedCaseId = caseId.trim();
    const normalizedTerm = term.trim();

    if (!session || !normalizedCaseId) {
      this.error.set('Abra uma sessão de viewer antes de pesquisar.');
      return;
    }

    if (!session.capabilities.search) {
      this.error.set('O viewer atual não suporta busca interna.');
      return;
    }

    if (!normalizedTerm) {
      this.searchTerm.set('');
      this.hitState.set(null);
      return;
    }

    this.searching.set(true);
    this.error.set(null);

    try {
      const response = await firstValueFrom(
        this.viewerService.searchInViewerSession({
          caseId: normalizedCaseId,
          viewerSessionId: session.viewerSessionId,
          viewerSearchRequest: {
            term: normalizedTerm,
            matchMode: ViewerSearchRequestMatchModeEnum.contains,
            caseSensitive: false
          }
        })
      );

      this.searchTerm.set(normalizedTerm);
      this.hitState.set({
        totalHits: response.totalHits,
        currentHit: response.currentHit
      });
    } catch (error) {
      this.error.set(this.toMessage(error, 'Falha ao pesquisar dentro do viewer.'));
    } finally {
      this.searching.set(false);
    }
  }

  async navigate(caseId: string, direction: 'next' | 'prev') {
    const session = this.session();
    const normalizedCaseId = caseId.trim();

    if (!session || !normalizedCaseId) {
      this.error.set('Abra uma sessão de viewer antes de navegar hits.');
      return;
    }

    this.searching.set(true);
    this.error.set(null);

    try {
      const hitState = await firstValueFrom(
        this.viewerService.navigateViewerHit({
          caseId: normalizedCaseId,
          viewerSessionId: session.viewerSessionId,
          viewerNavigateHitRequest: {
            direction:
              direction === 'next'
                ? ViewerNavigateHitRequestDirectionEnum.next
                : ViewerNavigateHitRequestDirectionEnum.prev,
            wrap: true
          }
        })
      );

      this.hitState.set(hitState);
    } catch (error) {
      this.error.set(this.toMessage(error, 'Falha ao navegar entre hits do viewer.'));
    } finally {
      this.searching.set(false);
    }
  }

  private async loadRendition(caseId: string, itemId: string, kind: ViewerRenderableKind) {
    this.kind.set(kind);

    switch (kind) {
      case 'text': {
        const text = await firstValueFrom(
          this.itemsService.getTextRendition({
            caseId,
            itemId
          })
        );
        this.textContent.set(text);
        return;
      }
      case 'html': {
        const html = await firstValueFrom(
          this.itemsService.getHtmlRendition({
            caseId,
            itemId
          })
        );
        this.htmlContent.set(this.sanitizer.bypassSecurityTrustHtml(html));
        return;
      }
      case 'image': {
        const image = await firstValueFrom(
          this.itemsService.getImageRendition({
            caseId,
            itemId
          })
        );
        this.resourceUrl.set(this.createObjectUrl(image));
        return;
      }
      case 'pdf': {
        const pdf = await firstValueFrom(
          this.itemsService.getPdfRendition({
            caseId,
            itemId
          })
        );
        this.resourceUrl.set(this.createObjectUrl(pdf));
        return;
      }
      default:
        this.error.set('Sem rendition suportada disponível para o item selecionado.');
    }
  }

  private chooseRenditionKind(renditions: Array<RenditionLink>, mimeType: string): ViewerRenderableKind {
    const kinds = new Set(renditions.map((entry) => entry.kind));

    if (kinds.has(RenditionLinkKindEnum.html)) {
      return 'html';
    }

    if (kinds.has(RenditionLinkKindEnum.text)) {
      return 'text';
    }

    if (kinds.has(RenditionLinkKindEnum.image)) {
      return 'image';
    }

    if (kinds.has(RenditionLinkKindEnum.pdf)) {
      return 'pdf';
    }

    if (mimeType.startsWith('text/')) {
      return 'text';
    }

    if (mimeType.includes('html')) {
      return 'html';
    }

    if (mimeType.startsWith('image/')) {
      return 'image';
    }

    if (mimeType === 'application/pdf') {
      return 'pdf';
    }

    return 'unsupported';
  }

  private createObjectUrl(blob: Blob) {
    this.revokeObjectUrl();
    this.objectUrl = URL.createObjectURL(blob);
    return this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl);
  }

  private resetRenderedContent() {
    this.textContent.set(null);
    this.htmlContent.set(null);
    this.resourceUrl.set(null);
    this.revokeObjectUrl();
  }

  private revokeObjectUrl() {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }

  private toMessage(error: unknown, fallback: string) {
    if (error instanceof Error && error.message) {
      return error.message;
    }

    return fallback;
  }
}
