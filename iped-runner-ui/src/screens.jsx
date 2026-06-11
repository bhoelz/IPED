import { useState } from 'react'
import { Icon } from './icons.jsx'
import { Card, Field, TextInput, Select, FilePicker, PasswordRow, FlagSwitch, FlagCheck, Segmented, TagsInput, NumberPresets, KeyValueGrid, EmptyNote, Pill } from './ui.jsx'
import { DATASOURCE_TYPES, PROFILES, TIMEZONES, INITIAL_STATE, applyProfile } from './command.js'

function PageHeader({ icon, title, desc, action }) {
  return (
    <div style={{ marginBottom: 26 }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
        <span style={{ color: 'var(--accent)', display: 'inline-flex', marginTop: 4 }}>
          <Icon name={icon} size={30} stroke={1.8} />
        </span>
        <div style={{ flex: 1 }}>
          <h1 style={{ margin: 0, fontSize: 30, fontWeight: 700, color: 'var(--accent-text)', letterSpacing: '.3px' }}>{title}</h1>
          <p style={{ margin: '6px 0 0', color: 'var(--text-2)', fontSize: 14.5, maxWidth: '72ch' }}>{desc}</p>
        </div>
        {action}
      </div>
    </div>
  )
}

export function ScreenEntradas({ s, patch, t, getIssue, openBrowser }) {
  const tt = t.entradas
  const setDs = (i, field, val) => patch({ datasources: s.datasources.map((d, idx) => idx === i ? { ...d, [field]: val } : d) })
  const addDs = () => patch({ datasources: [...s.datasources, { path: '', type: 'auto', dname: '' }] })
  const rmDs = (i) => patch({ datasources: s.datasources.filter((_, idx) => idx !== i) })

  const setPw = (i, val) => patch({ passwords: s.passwords.map((p, idx) => idx === i ? val : p) })
  const addPw = () => patch({ passwords: [...s.passwords, ''] })
  const rmPw = (i) => patch({ passwords: s.passwords.filter((_, idx) => idx !== i) })

  return (
    <>
      <PageHeader icon="login" title={tt.title} desc={tt.desc} />

      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        <Card title={tt.sourcesTitle} flag="-d, -data" desc={tt.sourcesDesc}
          action={<button className="btn btn-soft btn-sm" onClick={addDs}><Icon name="plus" size={15} /> {tt.addSource}</button>}>
          {s.datasources.length === 0 ? (
            <EmptyNote>{tt.noSources}</EmptyNote>
          ) : (
            <div className="repeat-list">
              {s.datasources.map((d, i) => {
                const empty = !d.path.trim() && (d.dname.trim() || d.type !== 'auto')
                return (
                  <div className="repeat-row" key={i}>
                    <div className="ds-row">
                      <div className="ds-path">
                        <Field label={tt.sourcePath} required
                          error={empty ? t.validation.sourceEmpty : null}
                          hint={d.path.trim() ? { kind: 'ok', icon: 'check', text: t.validation.sourceHint } : null}>
                          <TextInput value={d.path} onChange={(v) => setDs(i, 'path', v)} placeholder={tt.selectPath}
                            invalid={empty} affix={<Icon name="folder" size={16} />}
                            onAffix={() => openBrowser(d.path, 'any', (p) => setDs(i, 'path', p))} />
                        </Field>
                      </div>
                      <div>
                        <Field label={tt.type}>
                          <Select value={d.type} onChange={(v) => setDs(i, 'type', v)} options={DATASOURCE_TYPES} />
                        </Field>
                      </div>
                      <div>
                        <Field label={<span>{tt.displayName} <span className="mono" style={{ color: 'var(--text-muted)', fontSize: 11.5 }}>(-dname)</span></span>}>
                          <TextInput value={d.dname} onChange={(v) => setDs(i, 'dname', v)} placeholder={tt.autoGen} />
                        </Field>
                      </div>
                      <div className="ds-trash">
                        <button className="icon-btn danger" onClick={() => rmDs(i)} title="Remove"><Icon name="trash" size={17} /></button>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </Card>

        <div className="grid-2">
          <Card title={tt.keywordsTitle} flag="-l" desc={tt.keywordsDesc}>
            <FilePicker fileName={s.keywords} buttonLabel={tt.chooseFile} emptyLabel={tt.noFile}
              onPick={() => openBrowser(s.keywords, 'file', (p) => patch({ keywords: p }))}
              onClear={() => patch({ keywords: '' })} />
            {s.keywords && <span className="hinttext ok" style={{ marginTop: 8 }}><Icon name="check" size={13} /> FileExistsValidator — OK</span>}
          </Card>

          <Card title={tt.asapTitle} flag="-asap" desc={tt.asapDesc}>
            <FilePicker fileName={s.asap} buttonLabel={tt.chooseFile} emptyLabel={tt.noFile}
              invalid={!!getIssue('asapExt')}
              onPick={() => openBrowser(s.asap, 'file', (p) => patch({ asap: p }))}
              onClear={() => patch({ asap: '' })} />
            {getIssue('asapExt') && <span className="hinttext err" style={{ marginTop: 8 }}><Icon name="alert" size={13} /> {t.validation.asapExt}</span>}
          </Card>
        </div>

        <Card title={tt.passwordsTitle} flag="-p" desc={tt.passwordsDesc}
          action={<button className="btn btn-soft btn-sm" onClick={addPw}><Icon name="plus" size={15} /> {tt.addPassword}</button>}>
          {s.passwords.length === 0 ? (
            <EmptyNote>{tt.noPasswords}</EmptyNote>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {s.passwords.map((p, i) => (
                <PasswordRow key={i} value={p} onChange={(v) => setPw(i, v)} onRemove={() => rmPw(i)} placeholder={tt.enterPassword} />
              ))}
            </div>
          )}
        </Card>
      </div>
    </>
  )
}

export function ScreenSaida({ s, patch, t, getIssue, openBrowser }) {
  const tt = t.saida
  const needOut = getIssue('needOutput')
  return (
    <>
      <PageHeader icon="terminal" title={tt.title} desc={tt.desc} />
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        <Card title={tt.outputTitle} flag="-o, -output" desc={tt.outputDesc}>
          <Field required error={needOut ? t.validation.needOutput : null}
            hint={s.output.trim() ? { kind: 'ok', icon: 'check', text: 'OK' } : null}>
            <TextInput value={s.output} onChange={(v) => patch({ output: v })} placeholder={tt.selectDir}
              invalid={!!needOut} affix={<Icon name="folder" size={16} />}
              onAffix={() => openBrowser(s.output, 'dir', (p) => patch({ output: p }))} />
          </Field>
        </Card>

        <div className="grid-2">
          <Card title={tt.logTitle} flag="-log" desc={tt.logDesc} icon={<Icon name="fileText" size={17} />}>
            <Field label={tt.logFile}>
              <TextInput value={s.logFile} onChange={(v) => patch({ logFile: v })} placeholder={tt.logFilePh}
                affix={<Icon name="folder" size={16} />}
                onAffix={() => openBrowser(s.logFile, 'any', (p) => patch({ logFile: p }))} />
            </Field>
            <div style={{ marginTop: 6 }}>
              <FlagSwitch name={tt.nologfile} flag="--nologfile" desc={tt.nologfileDesc} on={s.nologfile} onChange={(v) => patch({ nologfile: v })} />
            </div>
          </Card>

          <Card title={tt.caseMgmtTitle} desc={tt.caseMgmtDesc} icon={<Icon name="hardDrive" size={17} />}>
            <FlagSwitch name={tt.append} flag="--append" desc={tt.appendDesc} on={s.append} onChange={(v) => patch({ append: v })} />
            <div className="flag-row box">
              <div className="flag-main">
                <div className="flag-name">{tt.remove} <Pill>-remove</Pill></div>
                <div className="flag-sub" style={{ marginBottom: 9 }}>{tt.removeDesc}</div>
                <TextInput value={s.remove} onChange={(v) => patch({ remove: v })} placeholder={tt.removePh} />
              </div>
            </div>
          </Card>
        </div>
      </div>
    </>
  )
}

export function ScreenProcessamento({ s, patch, t, notify }) {
  const tt = t.proc
  const modeOpts = [
    { value: 'normal', label: tt.modeNormal },
    { value: 'continue', label: tt.modeContinue, flag: '--continue' },
    { value: 'restart', label: tt.modeRestart, flag: '--restart' },
  ]
  const modeDesc = s.mode === 'continue' ? tt.modeContinueDesc : s.mode === 'restart' ? tt.modeRestartDesc : tt.modeNormalDesc
  const onProfile = (v) => { patch(applyProfile(s, v)); notify(`${t.proc.profile}: ${v}`) }

  return (
    <>
      <PageHeader icon="cpu" title={tt.title} desc={tt.desc}
        action={<button className="btn-link" onClick={() => patch({ ...INITIAL_STATE })}><Icon name="rotateCcw" size={15} /> {t.restore}</button>} />

      <div className="grid-2">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          <Card title={tt.modeTitle} icon={<Icon name="play" size={16} />} bar>
            <Segmented value={s.mode} onChange={(v) => patch({ mode: v })} options={modeOpts} />
            <p className="card-desc tech" style={{ marginTop: 14, fontSize: 12.5 }}>{modeDesc}</p>
          </Card>

          <Card title={tt.mainTitle} icon={<Icon name="sliders" size={16} />} bar>
            <Field label={tt.profile} flag="-profile">
              <Select value={s.profile} onChange={onProfile} options={PROFILES} />
            </Field>
            <div style={{ height: 18 }} />
            <Field label={tt.blocksize} flag="-b">
              <NumberPresets value={s.blocksize} onChange={(v) => patch({ blocksize: v })} presets={[512, 1024, 2048, 4096]} placeholder="4096" />
            </Field>
          </Card>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          <Card title={tt.ocrTitle} icon={<Icon name="filter" size={16} />} bar>
            <Field label={tt.ocrCats} flag="-ocr">
              <TagsInput tags={s.ocr} onChange={(v) => patch({ ocr: v })} placeholder={tt.ocrPh} />
            </Field>
            <div style={{ height: 18 }} />
            <Field label={tt.nocontent} flag="-nocontent">
              <TagsInput tags={s.nocontent} onChange={(v) => patch({ nocontent: v })} placeholder={tt.nocontentPh} />
            </Field>
          </Card>

          <Card title={tt.flagsTitle} icon={<Icon name="flag" size={16} />} bar>
            <FlagSwitch name={tt.addowner} flag="--addowner" desc={tt.addownerDesc} on={s.addowner} onChange={(v) => patch({ addowner: v })} />
            <FlagSwitch name={tt.downloadInternet} flag="--downloadInternetData" desc={tt.downloadInternetDesc} on={s.downloadInternet} onChange={(v) => patch({ downloadInternet: v })} />
          </Card>
        </div>
      </div>
    </>
  )
}

export function ScreenRelatorio({ s, patch, t }) {
  const tt = t.relatorio
  return (
    <>
      <PageHeader icon="fileText" title={tt.title} desc={tt.desc} />
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: 920 }}>
        <Card title={tt.exportTitle} desc={tt.exportDesc} icon={<Icon name="mail" size={17} />} bar>
          <FlagSwitch name={tt.nopstattachs} flag="--nopstattachs" desc={tt.nopstattachsDesc} on={s.nopstattachs} onChange={(v) => patch({ nopstattachs: v })} />
          <FlagSwitch name={tt.nolinkeditems} flag="--nolinkeditems" desc={tt.nolinkeditemsDesc} on={s.nolinkeditems} onChange={(v) => patch({ nolinkeditems: v })} />
        </Card>
        <Card title={tt.pathsTitle} desc={tt.pathsDesc} icon={<Icon name="link" size={17} />} bar>
          <FlagSwitch name={tt.portable} flag="--portable" desc={tt.portableDesc} on={s.portable} onChange={(v) => patch({ portable: v })} />
        </Card>
      </div>
    </>
  )
}

export function ScreenAvancado({ s, patch, t }) {
  const tt = t.avancado
  return (
    <>
      <PageHeader icon="settings" title={tt.title} desc={tt.desc} />
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: 1000 }}>
        <Card title={tt.envTitle} icon={<Icon name="globe" size={16} />} bar>
          <Field label={tt.timezone} flag="-tz" hint={{ text: tt.timezoneDesc }}>
            <Select value={s.timezone} onChange={(v) => patch({ timezone: v })} options={TIMEZONES.filter((z) => !z.disabled)} />
          </Field>
          <div style={{ height: 18 }} />
          <Field label={tt.splash} flag="-splash">
            <TextInput value={s.splash} onChange={(v) => patch({ splash: v })} placeholder={tt.splashPh}
              affix={<Icon name="message" size={15} />} onAffix={() => {}} mono={false} />
          </Field>
          <div style={{ marginTop: 14, paddingTop: 14, borderTop: '1px solid var(--border)' }}>
            <FlagCheck name={tt.nogui} flag="--nogui" desc={tt.noguiDesc} on={s.nogui} onChange={(v) => patch({ nogui: v })} />
          </div>
        </Card>

        <Card title={tt.extraTitle} flag="-X" desc={tt.extraDesc} icon={<Icon name="braces" size={16} />} bar>
          <KeyValueGrid rows={s.extra} onChange={(rows) => patch({ extra: rows })}
            headK={tt.key} headV={tt.value} phK={tt.enterKey} phV={tt.enterValue} />
        </Card>
      </div>
    </>
  )
}
