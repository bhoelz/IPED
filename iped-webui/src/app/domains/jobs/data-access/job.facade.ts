import { Injectable, computed, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { JobsService } from '../../../core/api/generated/api/jobs.service';
import {
  ExportJobRequestFormatEnum
} from '../../../core/api/generated/model/exportJobRequest';
import {
  JobAcceptedResponseStatusEnum
} from '../../../core/api/generated/model/jobAcceptedResponse';
import { JobStatus, JobStatusStatusEnum } from '../../../core/api/generated/model/jobStatus';

const JOB_POLL_INTERVAL_MS = 3_000;
const TERMINAL_JOB_STATUSES = new Set<JobStatusStatusEnum>([
  JobStatusStatusEnum.completed,
  JobStatusStatusEnum.failed,
  JobStatusStatusEnum.cancelled
]);

export interface TrackedJob {
  readonly jobId: string;
  readonly scopeLabel: string;
  readonly format: ExportJobRequestFormatEnum;
  readonly itemCount: number;
  readonly createdAt: string;
  readonly status: JobStatusStatusEnum;
  readonly progress: number | null;
  readonly message: string | null;
  readonly outputUrl: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class JobFacade {
  readonly jobs = signal<Array<TrackedJob>>([]);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly hasJobs = computed(() => this.jobs().length > 0);
  readonly hasFinishedJobs = computed(() =>
    this.jobs().some((job) => TERMINAL_JOB_STATUSES.has(job.status))
  );

  private readonly pollHandles = new Map<string, ReturnType<typeof setTimeout>>();

  constructor(private readonly jobsService: JobsService) {}

  async startExport(options: {
    caseId: string;
    itemIds: Array<string>;
    format: ExportJobRequestFormatEnum;
    scopeLabel: string;
  }) {
    const caseId = options.caseId.trim();
    const itemIds = [...new Set(options.itemIds.map((itemId) => itemId.trim()).filter(Boolean))];

    if (!caseId) {
      this.error.set('Abra um caso antes de iniciar uma exportação.');
      return false;
    }

    if (itemIds.length === 0) {
      this.error.set('Selecione pelo menos um item exportável antes de iniciar o job.');
      return false;
    }

    this.submitting.set(true);
    this.error.set(null);

    try {
      const accepted = await firstValueFrom(
        this.jobsService.createExportJob({
          exportJobRequest: {
            caseId,
            itemIds,
            format: options.format
          }
        })
      );

      this.upsertJob({
        jobId: accepted.jobId,
        scopeLabel: options.scopeLabel,
        format: options.format,
        itemCount: itemIds.length,
        createdAt: new Date().toISOString(),
        status: this.toTrackedStatus(accepted.status),
        progress: null,
        message: 'Job aceito e aguardando atualização de status.',
        outputUrl: null
      });

      await this.refreshJob(accepted.jobId);
      return true;
    } catch (error) {
      this.error.set(this.toMessage(error, 'Falha ao iniciar a exportação.'));
      return false;
    } finally {
      this.submitting.set(false);
    }
  }

  clearFinished() {
    const activeIds = new Set<string>();

    this.jobs.set(
      this.jobs().filter((job) => {
        if (!TERMINAL_JOB_STATUSES.has(job.status)) {
          activeIds.add(job.jobId);
          return true;
        }

        this.clearPoll(job.jobId);
        return false;
      })
    );

    for (const jobId of this.pollHandles.keys()) {
      if (!activeIds.has(jobId)) {
        this.clearPoll(jobId);
      }
    }
  }

  clear() {
    for (const jobId of this.pollHandles.keys()) {
      this.clearPoll(jobId);
    }

    this.jobs.set([]);
    this.error.set(null);
    this.submitting.set(false);
  }

  private async refreshJob(jobId: string) {
    try {
      const status = await firstValueFrom(
        this.jobsService.getJobStatus({
          jobId
        })
      );

      this.applyJobStatus(status);
    } catch (error) {
      this.error.set(this.toMessage(error, 'Falha ao atualizar o status do job.'));
      this.schedulePoll(jobId);
    }
  }

  private applyJobStatus(status: JobStatus) {
    this.upsertJob({
      jobId: status.jobId,
      status: status.status,
      progress: status.progress ?? null,
      message: status.message ?? null,
      outputUrl: status.outputUrl ?? null
    });

    if (TERMINAL_JOB_STATUSES.has(status.status)) {
      this.clearPoll(status.jobId);
      return;
    }

    this.schedulePoll(status.jobId);
  }

  private upsertJob(job: Partial<TrackedJob> & Pick<TrackedJob, 'jobId'>) {
    const jobs = [...this.jobs()];
    const index = jobs.findIndex((candidate) => candidate.jobId === job.jobId);

    if (index >= 0) {
      jobs[index] = {
        ...jobs[index],
        ...job
      };
    } else {
      jobs.unshift({
        jobId: job.jobId,
        scopeLabel: job.scopeLabel ?? 'Escopo não informado',
        format: job.format ?? ExportJobRequestFormatEnum.zip,
        itemCount: job.itemCount ?? 0,
        createdAt: job.createdAt ?? new Date().toISOString(),
        status: job.status ?? JobStatusStatusEnum.accepted,
        progress: job.progress ?? null,
        message: job.message ?? null,
        outputUrl: job.outputUrl ?? null
      });
    }

    this.jobs.set(jobs);
  }

  private schedulePoll(jobId: string) {
    this.clearPoll(jobId);

    const handle = setTimeout(() => {
      void this.refreshJob(jobId);
    }, JOB_POLL_INTERVAL_MS);

    this.pollHandles.set(jobId, handle);
  }

  private clearPoll(jobId: string) {
    const handle = this.pollHandles.get(jobId);

    if (handle) {
      clearTimeout(handle);
      this.pollHandles.delete(jobId);
    }
  }

  private toTrackedStatus(status: JobAcceptedResponseStatusEnum) {
    return status === JobAcceptedResponseStatusEnum.running
      ? JobStatusStatusEnum.running
      : JobStatusStatusEnum.accepted;
  }

  private toMessage(error: unknown, fallback: string) {
    if (error instanceof Error && error.message) {
      return error.message;
    }

    return fallback;
  }
}
