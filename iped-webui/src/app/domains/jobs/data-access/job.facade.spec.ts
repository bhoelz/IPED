import {TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {vi} from 'vitest';

import {JobsService} from '../../../core/api/generated/api/jobs.service';
import {ExportJobRequestFormatEnum} from '../../../core/api/generated/model/exportJobRequest';
import {JobStatusStatusEnum} from '../../../core/api/generated/model/jobStatus';
import {JobFacade} from './job.facade';

describe('JobFacade', () => {
  it('should reject export when no item is selected', async () => {
    TestBed.configureTestingModule({
      providers: [
        JobFacade,
        {
          provide: JobsService,
          useValue: {
            createExportJob: vi.fn(),
            getJobStatus: vi.fn()
          }
        }
      ]
    });

    const facade = TestBed.inject(JobFacade);

    const started = await facade.startExport({
      caseId: 'demo-case',
      itemIds: [],
      format: ExportJobRequestFormatEnum.zip,
      scopeLabel: 'Item selecionado'
    });

    expect(started).toBe(false);
    expect(facade.error()).toContain('Selecione pelo menos um item export');
    expect(facade.jobs()).toHaveLength(0);
  });

  it('should track a completed export job', async () => {
    const jobsService = {
      createExportJob: vi.fn().mockReturnValue(
        of({
          jobId: 'job-1',
          status: 'accepted'
        })
      ),
      getJobStatus: vi.fn().mockReturnValue(
        of({
          jobId: 'job-1',
          status: JobStatusStatusEnum.completed,
          progress: 1,
          message: 'Export pronto',
          outputUrl: '/downloads/export.zip'
        })
      )
    };

    TestBed.configureTestingModule({
      providers: [
        JobFacade,
        {
          provide: JobsService,
          useValue: jobsService
        }
      ]
    });

    const facade = TestBed.inject(JobFacade);

    const started = await facade.startExport({
      caseId: 'demo-case',
      itemIds: ['item-1', 'item-1', 'item-2'],
      format: ExportJobRequestFormatEnum.zip,
      scopeLabel: 'Pagina atual'
    });

    expect(started).toBe(true);
    expect(jobsService.createExportJob).toHaveBeenCalledWith({
      exportJobRequest: {
        caseId: 'demo-case',
        itemIds: ['item-1', 'item-2'],
        format: ExportJobRequestFormatEnum.zip
      }
    });
    expect(facade.jobs()).toEqual([
      expect.objectContaining({
        jobId: 'job-1',
        scopeLabel: 'Pagina atual',
        itemCount: 2,
        status: JobStatusStatusEnum.completed,
        outputUrl: '/downloads/export.zip'
      })
    ]);
    expect(facade.hasFinishedJobs()).toBe(true);
  });
});
