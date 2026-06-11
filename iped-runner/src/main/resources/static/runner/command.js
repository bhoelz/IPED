// Data model, command assembly, validation, and profile presets for IPED Runner

const DATASOURCE_TYPES = [
  { value: 'auto', label: 'Auto-detectar' },
  { value: 'dd', label: 'Raw (.DD / .001)' },
  { value: 'e01', label: 'EnCase (.E01 / .Ex01)' },
  { value: 'aff', label: 'AFF (Linux)' },
  { value: 'ad1', label: 'AD1 (AccessData)' },
  { value: 'vhd', label: 'VHD / VHDX' },
  { value: 'vmdk', label: 'VMDK (VMware)' },
  { value: 'iso', label: 'ISO (.iso)' },
  { value: 'ufdr', label: 'UFDR (Cellebrite)' },
  { value: 'physical', label: 'Physical Drive' },
  { value: 'folder', label: 'Pasta / Folder' },
  { value: 'iped', label: 'Caso IPED (*.iped)' },
];

const PROFILES = [
  { value: 'forensic', label: 'forensic — Deep Analysis' },
  { value: 'pedo', label: 'pedo — CSAM Triage' },
  { value: 'fastmode', label: 'fastmode — Quick Scan' },
  { value: 'blind', label: 'blind — No Preview' },
  { value: 'triage', label: 'triage — Field Triage' },
];

const PROFILE_PRESETS = {
  forensic: { ocr: ['Documents', 'Images'], nocontent: [], addowner: true, downloadInternet: false, blocksize: '4096', nogui: false, nopstattachs: false, nolinkeditems: false },
  pedo:     { ocr: ['Images', 'Videos'], nocontent: ['System_Files'], addowner: false, downloadInternet: false, blocksize: '4096', nogui: false, nopstattachs: false, nolinkeditems: false },
  fastmode: { ocr: [], nocontent: ['System_Files'], addowner: false, downloadInternet: false, blocksize: '512', nogui: false, nopstattachs: true, nolinkeditems: true },
  blind:    { ocr: [], nocontent: [], addowner: false, downloadInternet: false, blocksize: '4096', nogui: true, nopstattachs: true, nolinkeditems: true },
  triage:   { ocr: [], nocontent: ['System_Files'], addowner: false, downloadInternet: false, blocksize: '512', nogui: true, nopstattachs: true, nolinkeditems: true },
};

function buildTimezones() {
  const named = [
    { value: '', label: '(local — usar fuso do sistema)' },
    { value: 'America/Sao_Paulo (GMT-3)', label: 'America/Sao_Paulo (GMT-3)' },
    { value: 'America/New_York (GMT-5)', label: 'America/New_York (GMT-5)' },
    { value: 'America/Manaus (GMT-4)', label: 'America/Manaus (GMT-4)' },
    { value: 'Europe/Lisbon (GMT+0)', label: 'Europe/Lisbon (GMT+0)' },
    { value: 'UTC', label: 'UTC (GMT+0)' },
  ];
  const offsets = [];
  for (let i = -12; i <= 14; i++) {
    const s = i === 0 ? 'GMT+0' : `GMT${i > 0 ? '+' : ''}${i}`;
    offsets.push({ value: s, label: s });
  }
  return [...named, { value: '__sep', label: '──────────', disabled: true }, ...offsets];
}
const TIMEZONES = buildTimezones();

const INITIAL_STATE = {
  datasources: [
    { path: 'E:\\Images\\Evidence_001.E01', type: 'e01', dname: 'Evidence_001' },
    { path: '', type: 'auto', dname: '' },
  ],
  keywords: '',
  asap: '',
  passwords: ['Pa55w0rd!', ''],
  output: '',
  logFile: '',
  nologfile: false,
  append: false,
  remove: '',
  mode: 'normal',
  profile: 'forensic',
  blocksize: '4096',
  ocr: ['Documents', 'Images'],
  nocontent: ['System_Files'],
  addowner: true,
  downloadInternet: false,
  nopstattachs: false,
  nolinkeditems: false,
  portable: true,
  timezone: 'America/Sao_Paulo (GMT-3)',
  splash: '',
  nogui: false,
  extra: [
    { k: 'java.net.preferIPv4Stack', v: 'true' },
    { k: 'log4j2.formatMsgNoLookups', v: 'true' },
    { k: 'file.encoding', v: 'UTF-8' },
  ],
};

function tzArg(tz) {
  const m = tz.match(/(GMT[+-]?\d+)/);
  return m ? m[1] : tz;
}

function q(v) {
  return /\s/.test(v) ? `"${v}"` : v;
}

function buildCommand(s) {
  const t = [];
  const push = (flag, val) => t.push({ flag, val: val === undefined ? null : val });

  s.datasources.forEach((d) => {
    if (d.path.trim()) {
      push('-d', q(d.path.trim()));
      if (d.dname.trim()) push('-dname', q(d.dname.trim()));
    }
  });
  if (s.keywords) push('-l', q(s.keywords));
  if (s.asap) push('-asap', q(s.asap));
  s.passwords.forEach((p) => { if (p.trim()) push('-p', q(p.trim())); });

  if (s.output.trim()) push('-o', q(s.output.trim()));
  if (s.logFile.trim()) push('-log', q(s.logFile.trim()));
  if (s.nologfile) push('--nologfile');
  if (s.append) push('--append');
  if (s.remove.trim()) push('-remove', q(s.remove.trim()));

  if (s.mode === 'continue') push('--continue');
  if (s.mode === 'restart') push('--restart');

  if (s.profile) push('-profile', s.profile);
  if (s.blocksize) push('-b', String(s.blocksize));
  s.ocr.forEach((c) => push('-ocr', q(c)));
  s.nocontent.forEach((c) => push('-nocontent', q(c)));
  if (s.addowner) push('--addowner');
  if (s.downloadInternet) push('--downloadInternetData');

  if (s.nopstattachs) push('--nopstattachs');
  if (s.nolinkeditems) push('--nolinkeditems');
  if (s.portable) push('--portable');

  if (s.timezone) push('-tz', tzArg(s.timezone));
  if (s.splash.trim()) push('-splash', q(s.splash.trim()));
  if (s.nogui) push('--nogui');

  s.extra.forEach((e) => { if (e.k.trim()) push(`-X${e.k.trim()}=${e.v.trim()}`); });

  const str = 'iped ' + t.map((x) => (x.val !== null && x.val !== undefined ? `${x.flag} ${x.val}` : x.flag)).join(' ');
  return { tokens: t, str };
}

function validateState(s) {
  const issues = [];
  const hasSource = s.datasources.some((d) => d.path.trim());
  if (!hasSource) issues.push({ id: 'needSource', screen: 'entradas', level: 'error', key: 'needSource' });
  s.datasources.forEach((d, i) => {
    if (!d.path.trim() && (d.dname.trim() || d.type !== 'auto')) {
      issues.push({ id: 'srcEmpty-' + i, screen: 'entradas', level: 'error', key: 'sourceEmpty' });
    }
  });
  if (s.asap && !/\.asap$/i.test(s.asap)) issues.push({ id: 'asapExt', screen: 'entradas', level: 'error', key: 'asapExt' });
  s.passwords.forEach((p, i) => {
    if (i < s.passwords.length - 1 && !p.trim()) issues.push({ id: 'pw-' + i, screen: 'entradas', level: 'warn', key: 'passwordEmpty' });
  });

  if (!s.output.trim()) issues.push({ id: 'needOutput', screen: 'saida', level: 'error', key: 'needOutput' });

  s.extra.forEach((e, i) => {
    if (!e.k.trim() && e.v.trim()) issues.push({ id: 'xk-' + i, screen: 'avancado', level: 'warn', key: 'extraKeyEmpty' });
  });

  return issues;
}

function applyProfile(state, profileId) {
  const p = PROFILE_PRESETS[profileId];
  if (!p) return { profile: profileId };
  return { ...p, profile: profileId };
}

Object.assign(window, {
  DATASOURCE_TYPES, PROFILES, PROFILE_PRESETS, TIMEZONES, INITIAL_STATE,
  buildCommand, validateState, applyProfile, tzArg,
});
