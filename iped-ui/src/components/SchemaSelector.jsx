import React from 'react';

function SchemaSelector({
  groupedSchemas,
  selectedSchema,
  expandedCategories,
  onSelectSchema,
  onToggleCategory,
  loading
}) {
  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-icon">A</div>
        <div>
          <div className="sidebar-title">System Architect</div>
          <div className="sidebar-subtitle">COMPONENT CONFIGURATOR</div>
        </div>
      </div>

      {loading && <div style={{ padding: '16px', color: '#999' }}>Loading...</div>}

      {!loading && Object.keys(groupedSchemas).length === 0 && (
        <div style={{ padding: '16px', color: '#999' }}>No components available</div>
      )}

      {!loading &&
        Object.keys(groupedSchemas).map(category => (
          <div key={category}>
            <div
              className="tree-node"
              onClick={() => onToggleCategory(category)}
            >
              <span className="tree-toggle">
                {expandedCategories[category] ? '▼' : '▶'}
              </span>
              <span>{category}</span>
            </div>

            {expandedCategories[category] && (
              <div className="tree-children">
                {groupedSchemas[category].map(schema => (
                  <div
                    key={schema.name}
                    className={`tree-child-node ${
                      selectedSchema?.name === schema.name ? 'selected' : ''
                    }`}
                    onClick={() => onSelectSchema(schema.name)}
                  >
                    {schema.name}
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
    </div>
  );
}

export default SchemaSelector;
