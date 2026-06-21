# iped-distributed

Kafka-based distributed processing — coordinator, processing agents, status reporting (`iped.distributed.{agent,config,coordinator,kafka,status}`). Implements workstream 4 of the root [ROADMAP.md](../../ROADMAP.md).

## Roadmap

See [iped-distributed-ROADMAP.md](../../docs/roadmaps/iped-distributed-ROADMAP.md) for planned work, current phase status, and linked issues.

## Further reading

- [PROCESSING.md](PROCESSING.md) — original design rationale (Kafka topic layout, item serialization, task-agent model). Most of it is already implemented.
- [DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md) — operational deployment guide (Kafka setup, security hardening, monitoring, rollout checklist).
