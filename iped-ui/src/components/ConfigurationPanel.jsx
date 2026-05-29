import React from 'react';

function renderField(fieldName, fieldSchema, value, onChange, error, uiSchema = {}) {
  const fieldUI = uiSchema[fieldName] || {};
  const baseProps = {
    className: error ? 'error' : '',
    onChange: (e) => {
      const newValue = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
      onChange(fieldName, newValue);
    }
  };

  if (fieldSchema.type === 'boolean' || fieldSchema.type === 'object' && fieldSchema.properties?.active) {
    return (
      <div className="toggle-group" key={fieldName}>
        <span>{fieldName}</span>
        <button
          className={`toggle-switch ${value ? 'active' : ''}`}
          onClick={() => onChange(fieldName, !value)}
          type="button"
        />
      </div>
    );
  }

  if (fieldSchema.type === 'number' || fieldSchema.type === 'integer') {
    if (fieldUI['ui:widget'] === 'range') {
      const min = fieldSchema.minimum || 0;
      const max = fieldSchema.maximum || 100;
      return (
        <div className="form-group" key={fieldName}>
          <label className="form-label">{fieldName}</label>
          {fieldUI['ui:help'] && <div className="form-hint">{fieldUI['ui:help']}</div>}
          <div className="slider-group">
            <input
              type="range"
              {...baseProps}
              min={min}
              max={max}
              value={value || min}
              style={{ flex: 1 }}
            />
            <span className="slider-value">{value || min}</span>
          </div>
          {fieldUI['ui:labels'] && (
            <div className="slider-labels">
              <span>{fieldUI['ui:labels'][0]}</span>
              <span>{fieldUI['ui:labels'][1]}</span>
            </div>
          )}
          {error && <div className="error-message">{error}</div>}
        </div>
      );
    }
    return (
      <div className="form-group" key={fieldName}>
        <label className="form-label">{fieldName}</label>
        {fieldUI['ui:help'] && <div className="form-hint">{fieldUI['ui:help']}</div>}
        <input
          type="number"
          {...baseProps}
          value={value || ''}
          placeholder={fieldUI['ui:placeholder'] || ''}
        />
        {error && <div className="error-message">{error}</div>}
      </div>
    );
  }

  if (fieldSchema.enum) {
    return (
      <div className="form-group" key={fieldName}>
        <label className="form-label">{fieldName}</label>
        {fieldUI['ui:help'] && <div className="form-hint">{fieldUI['ui:help']}</div>}
        <select {...baseProps} value={value || ''}>
          <option value="">-- Select --</option>
          {fieldSchema.enum.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
        {error && <div className="error-message">{error}</div>}
      </div>
    );
  }

  if (fieldUI['ui:widget'] === 'textarea') {
    return (
      <div className="form-group" key={fieldName}>
        <label className="form-label">{fieldName}</label>
        {fieldUI['ui:help'] && <div className="form-hint">{fieldUI['ui:help']}</div>}
        <textarea
          {...baseProps}
          value={value || ''}
          placeholder={fieldUI['ui:placeholder'] || ''}
        />
        {error && <div className="error-message">{error}</div>}
      </div>
    );
  }

  return (
    <div className="form-group" key={fieldName}>
      <label className="form-label">{fieldName}</label>
      {fieldUI['ui:help'] && <div className="form-hint">{fieldUI['ui:help']}</div>}
      <input
        type="text"
        {...baseProps}
        value={value || ''}
        placeholder={fieldUI['ui:placeholder'] || ''}
      />
      {error && <div className="error-message">{error}</div>}
    </div>
  );
}

function ConfigurationPanel({
  schema,
  formData,
  errors,
  onFormDataChange,
  onErrorsChange
}) {
  if (!schema) return null;

  const properties = schema.jsonSchema?.properties || {};
  const uiSchema = schema.uiSchema || {};

  return (
    <div className="main-panel">
      <div className="component-header">
        <h1 className="component-title">{schema.name}</h1>
        <span className="breadcrumb">Configuration</span>
        <div className="component-badges">
          <span className="badge">editable</span>
        </div>
      </div>

      <div>
        {Object.keys(properties).length === 0 ? (
          <p style={{ color: '#999' }}>No configuration properties available</p>
        ) : (
          Object.keys(properties).map((fieldName) =>
            renderField(
              fieldName,
              properties[fieldName],
              formData[fieldName],
              onFormDataChange,
              errors[fieldName],
              uiSchema
            )
          )
        )}
      </div>
    </div>
  );
}

export default ConfigurationPanel;
