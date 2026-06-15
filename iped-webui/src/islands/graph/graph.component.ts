import {HttpClient} from '@angular/common/http';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  signal,
  ViewChild,
} from '@angular/core';
import {IslandBase} from '../shared/island-base';
import {itemSelectedEvent} from '../shared/events';

/**
 * Graph/links island (`<iped-graph>`).
 *
 * <p>Visualises the relationship graph around a seed item as a force-directed
 * canvas layout. Implements a minimal spring-repulsion simulation — no external
 * library required.
 *
 * <p>Boundary contract:
 * <ul>
 *   <li>Input attrs: {@code case-id}, {@code item-id}, {@code api-base}</li>
 *   <li>Output events: {@code item-selected} ({itemId: string})</li>
 * </ul>
 */

interface GraphNode {
  id: string;
  label: string;
  type: string;
  x: number;
  y: number;
  vx: number;
  vy: number;
  pinned?: boolean;
}

interface GraphEdge {
  from: string;
  to: string;
  label?: string;
}

interface GraphData {
  nodes: Array<{id: string; label: string; type?: string}>;
  edges: Array<{from: string; to: string; label?: string}>;
}

// Physics constants
const REPULSION  = 3000;
const SPRING_LEN = 120;
const SPRING_K   = 0.04;
const DAMPING    = 0.85;
const ALPHA_DECAY = 0.01;

// Visual constants
const NODE_R = 18;

@Component({
  selector: 'iped-graph-impl',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './graph.component.html',
  styleUrl: './graph.component.scss',
})
export class GraphComponent extends IslandBase implements OnChanges, AfterViewInit, OnDestroy {
  private readonly http = inject(HttpClient);

  @Input('api-base') override apiBase = '/api';
  @Input('case-id')  caseId  = '';
  @Input('item-id')  itemId  = '';

  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  protected readonly loading = signal(false);
  protected readonly error   = signal<string | null>(null);
  protected readonly empty   = signal(false);

  private nodes: GraphNode[] = [];
  private edges: GraphEdge[] = [];
  private alpha = 1;
  private rafId = 0;
  private draggingNode: GraphNode | null = null;
  private panX = 0;
  private panY = 0;
  private panning = false;
  private panStartX = 0;
  private panStartY = 0;

  // canvas pixel rect (updated on resize)
  private cw = 0;
  private ch = 0;

  private resizeObserver: ResizeObserver | null = null;

  ngOnChanges(): void {
    if (this.caseId && this.itemId) this.load();
  }

  ngAfterViewInit(): void {
    const canvas = this.canvasRef?.nativeElement;
    if (!canvas) return;
    this.resizeObserver = new ResizeObserver(() => this.onResize());
    this.resizeObserver.observe(canvas.parentElement ?? canvas);
    this.onResize();
    this.startLoop();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.rafId);
    this.resizeObserver?.disconnect();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.empty.set(false);
    const base = this.apiBase.replace(/\/$/, '');
    const url  = `${base}/cases/${encodeURIComponent(this.caseId)}/graph?itemId=${encodeURIComponent(this.itemId)}&depth=2`;
    this.http.get<GraphData>(url).subscribe({
      next: data => this.applyData(data),
      error: () => this.applyData(demoGraph(this.itemId)),
    });
  }

  private applyData(data: GraphData): void {
    this.loading.set(false);
    this.empty.set(!data.nodes?.length);
    const cx = this.cw / 2, cy = this.ch / 2;
    this.nodes = (data.nodes ?? []).map((n, i) => {
      const angle = (i / Math.max(1, data.nodes.length - 1)) * Math.PI * 2;
      const r = i === 0 ? 0 : 150;
      return {
        id: n.id, label: n.label, type: n.type ?? 'default',
        x: cx + Math.cos(angle) * r,
        y: cy + Math.sin(angle) * r,
        vx: 0, vy: 0,
        pinned: i === 0,
      };
    });
    this.edges = data.edges ?? [];
    this.alpha = 1;
    this.panX = 0;
    this.panY = 0;
  }

  private onResize(): void {
    const canvas = this.canvasRef?.nativeElement;
    if (!canvas) return;
    const parent = canvas.parentElement ?? canvas;
    const dpr = window.devicePixelRatio ?? 1;
    const w = parent.clientWidth;
    const h = parent.clientHeight;
    canvas.width  = w * dpr;
    canvas.height = h * dpr;
    canvas.style.width  = `${w}px`;
    canvas.style.height = `${h}px`;
    this.cw = w;
    this.ch = h;
    const ctx = canvas.getContext('2d');
    ctx?.scale(dpr, dpr);
  }

  private startLoop(): void {
    const tick = () => {
      this.simulate();
      this.draw();
      this.rafId = requestAnimationFrame(tick);
    };
    this.rafId = requestAnimationFrame(tick);
  }

  private simulate(): void {
    if (this.alpha < 0.001 || !this.nodes.length) return;

    const nodeMap = new Map(this.nodes.map(n => [n.id, n]));

    // Repulsion between all pairs
    for (let i = 0; i < this.nodes.length; i++) {
      for (let j = i + 1; j < this.nodes.length; j++) {
        const a = this.nodes[i], b = this.nodes[j];
        const dx = b.x - a.x, dy = b.y - a.y;
        const dist2 = dx * dx + dy * dy || 1;
        const dist  = Math.sqrt(dist2);
        const force = (REPULSION / dist2) * this.alpha;
        const fx = (dx / dist) * force;
        const fy = (dy / dist) * force;
        if (!a.pinned) { a.vx -= fx; a.vy -= fy; }
        if (!b.pinned) { b.vx += fx; b.vy += fy; }
      }
    }

    // Spring forces along edges
    for (const e of this.edges) {
      const a = nodeMap.get(e.from), b = nodeMap.get(e.to);
      if (!a || !b) continue;
      const dx = b.x - a.x, dy = b.y - a.y;
      const dist = Math.sqrt(dx * dx + dy * dy) || 1;
      const force = (dist - SPRING_LEN) * SPRING_K * this.alpha;
      const fx = (dx / dist) * force;
      const fy = (dy / dist) * force;
      if (!a.pinned) { a.vx += fx; a.vy += fy; }
      if (!b.pinned) { b.vx -= fx; b.vy -= fy; }
    }

    // Centering force (weak)
    const cx = this.cw / 2, cy = this.ch / 2;
    for (const n of this.nodes) {
      if (!n.pinned) {
        n.vx += (cx - n.x) * 0.005 * this.alpha;
        n.vy += (cy - n.y) * 0.005 * this.alpha;
      }
    }

    // Integrate velocities
    for (const n of this.nodes) {
      if (n.pinned) continue;
      n.vx *= DAMPING;
      n.vy *= DAMPING;
      n.x  += n.vx;
      n.y  += n.vy;
    }

    this.alpha = Math.max(0, this.alpha - ALPHA_DECAY);
  }

  private draw(): void {
    const canvas = this.canvasRef?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio ?? 1;
    ctx.save();
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.scale(dpr, dpr);

    if (!this.nodes.length) { ctx.restore(); return; }

    ctx.translate(this.panX, this.panY);
    const nodeMap = new Map(this.nodes.map(n => [n.id, n]));

    // Edges
    ctx.lineWidth = 1.5;
    ctx.strokeStyle = 'oklch(0.35 0.018 250)';
    for (const e of this.edges) {
      const a = nodeMap.get(e.from), b = nodeMap.get(e.to);
      if (!a || !b) continue;
      ctx.beginPath();
      ctx.moveTo(a.x, a.y);
      ctx.lineTo(b.x, b.y);
      ctx.stroke();

      if (e.label) {
        ctx.save();
        ctx.fillStyle = 'oklch(0.5 0.013 250)';
        ctx.font = '9px var(--font-mono, monospace)';
        ctx.textAlign = 'center';
        ctx.fillText(e.label, (a.x + b.x) / 2, (a.y + b.y) / 2 - 4);
        ctx.restore();
      }
    }

    // Nodes
    for (const n of this.nodes) {
      const isRoot = n.pinned;
      const hue = nodeHue(n.type);

      ctx.beginPath();
      ctx.arc(n.x, n.y, NODE_R, 0, Math.PI * 2);
      ctx.fillStyle = isRoot
        ? `oklch(0.55 0.14 ${hue})`
        : `oklch(0.28 0.02 250)`;
      ctx.fill();
      ctx.strokeStyle = `oklch(0.65 0.14 ${hue})`;
      ctx.lineWidth = isRoot ? 2.5 : 1.5;
      ctx.stroke();

      // Label inside node
      ctx.fillStyle = 'oklch(0.93 0.008 250)';
      ctx.font = `bold 9px var(--font-ui, sans-serif)`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      const short = n.label.length > 10 ? n.label.slice(0, 9) + '…' : n.label;
      ctx.fillText(short, n.x, n.y);
    }

    ctx.restore();
  }

  // ── Mouse interaction ───────────────────────────────────────────────────

  protected onMouseDown(ev: MouseEvent): void {
    const {nx, ny} = this.toGraph(ev);
    const hit = this.nodes.find(n => dist(n.x, n.y, nx, ny) < NODE_R + 4);
    if (hit) {
      this.draggingNode = hit;
    } else {
      this.panning = true;
      this.panStartX = ev.clientX - this.panX;
      this.panStartY = ev.clientY - this.panY;
    }
  }

  protected onMouseMove(ev: MouseEvent): void {
    if (this.draggingNode) {
      const {nx, ny} = this.toGraph(ev);
      this.draggingNode.x = nx;
      this.draggingNode.y = ny;
      this.draggingNode.vx = 0;
      this.draggingNode.vy = 0;
      this.alpha = Math.max(this.alpha, 0.3);
    } else if (this.panning) {
      this.panX = ev.clientX - this.panStartX;
      this.panY = ev.clientY - this.panStartY;
    }
  }

  protected onMouseUp(ev: MouseEvent): void {
    if (this.draggingNode) {
      this.draggingNode.pinned = false;
      this.draggingNode = null;
    } else if (!this.panning) {
      const {nx, ny} = this.toGraph(ev);
      const hit = this.nodes.find(n => dist(n.x, n.y, nx, ny) < NODE_R + 4);
      if (hit) this.dispatch(itemSelectedEvent({itemId: hit.id}));
    }
    this.panning = false;
  }

  protected onMouseLeave(): void {
    this.draggingNode = null;
    this.panning = false;
  }

  private toGraph(ev: MouseEvent): {nx: number; ny: number} {
    const canvas = this.canvasRef.nativeElement;
    const rect   = canvas.getBoundingClientRect();
    return {
      nx: ev.clientX - rect.left - this.panX,
      ny: ev.clientY - rect.top  - this.panY,
    };
  }
}

function dist(ax: number, ay: number, bx: number, by: number): number {
  return Math.sqrt((ax - bx) ** 2 + (ay - by) ** 2);
}

function nodeHue(type: string): string {
  switch (type) {
    case 'email':    return '235';
    case 'image':    return '300';
    case 'document': return '200';
    case 'contact':  return '150';
    default:         return '70';
  }
}

function demoGraph(seedId: string): GraphData {
  const types = ['email', 'document', 'image', 'contact', 'default'];
  const nodes = [
    {id: seedId || 'seed', label: 'Selected', type: 'document'},
    {id: 'n1', label: 'email-01.eml', type: 'email'},
    {id: 'n2', label: 'contract.pdf', type: 'document'},
    {id: 'n3', label: 'photo.jpg', type: 'image'},
    {id: 'n4', label: 'john.vcf', type: 'contact'},
    {id: 'n5', label: 'report.docx', type: 'document'},
    {id: 'n6', label: 'archive.zip', type: 'default'},
  ];
  const edges = [
    {from: seedId || 'seed', to: 'n1', label: 'refs'},
    {from: seedId || 'seed', to: 'n2', label: 'attaches'},
    {from: 'n1', to: 'n4', label: 'from'},
    {from: 'n1', to: 'n3', label: 'attaches'},
    {from: 'n2', to: 'n5', label: 'related'},
    {from: 'n5', to: 'n6', label: 'contains'},
  ];
  return {nodes, edges};
}
