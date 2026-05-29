import React from 'react';

function InfoPanel({ schema }) {
  if (!schema) return null;

  return (
    <div className="info-panel">
      <div className="info-section">
        <div className="info-section-title">Component Information</div>
        <div className="info-content">
          <div className="info-text">
            <strong>{schema.name}</strong>
            <br />
            <small style={{ color: '#999' }}>Configuration component</small>
          </div>
        </div>
      </div>

      {schema.jsonSchema?.description && (
        <div className="info-section">
          <div className="info-section-title">Description</div>
          <div className="info-content">
            <div className="info-text">{schema.jsonSchema.description}</div>
          </div>
        </div>
      )}

      <div className="info-section">
        <div className="info-section-title">Properties</div>
        <div className="info-content">
          <div className="info-text">
            {schema.jsonSchema?.properties ? (
              <>
                <strong>{Object.keys(schema.jsonSchema.properties).length}</strong> properties
              </>
            ) : (
              'No properties available'
            )}
          </div>
        </div>
      </div>

      <div className="info-section">
        <div className="info-section-title">Performance Metrics</div>
        <div className="metrics">
          <div className="metric">
            <div className="metric-value">—</div>
            <div className="metric-label">Load Time</div>
          </div>
          <div className="metric">
            <div className="metric-value">—</div>
            <div className="metric-label">Memory</div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default InfoPanel;
