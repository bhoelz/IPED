import React, { useState, useEffect } from 'react';
import './App.css';
import ConfigurationPanel from './components/ConfigurationPanel';
import SchemaSelector from './components/SchemaSelector';
import InfoPanel from './components/InfoPanel';
import Toast from './components/Toast';
import { configAPI } from './api/configAPI';

function App() {
  const [selectedSchema, setSelectedSchema] = useState(null);
  const [schemas, setSchemas] = useState([]);
  const [configurations, setConfigurations] = useState({});
  const [formData, setFormData] = useState({});
  const [errors, setErrors] = useState({});
  const [toast, setToast] = useState(null);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('Workspace');
  const [expandedCategories, setExpandedCategories] = useState({});

  // Load available schemas on mount
  useEffect(() => {
    loadSchemas();
    loadConfigurations();
  }, []);

  const loadSchemas = async () => {
    try {
      setLoading(true);
      const response = await configAPI.listSchemas();
      if (response.success) {
        setSchemas(response.schemas || []);
        // Initialize expanded categories
        const categories = {};
        response.schemas?.forEach(s => {
          if (!categories[s.category]) {
            categories[s.category] = true;
          }
        });
        setExpandedCategories(categories);
      }
    } catch (error) {
      showToast('Failed to load schemas', 'error');
      console.error('Error loading schemas:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadConfigurations = async () => {
    try {
      const response = await configAPI.getCurrentConfigurations();
      if (response.success) {
        setConfigurations(response.configurations || {});
      }
    } catch (error) {
      console.error('Error loading configurations:', error);
    }
  };

  const loadSchema = async (schemaName) => {
    try {
      setLoading(true);
      const response = await configAPI.getSchema(schemaName);
      if (response.success) {
        setSelectedSchema({
          name: schemaName,
          jsonSchema: response.schema,
          uiSchema: response.uiSchema
        });

        // Load configuration for this schema
        const configResponse = await configAPI.getConfiguration(schemaName);
        if (configResponse.success) {
          setFormData(configResponse.data || {});
        } else {
          setFormData({});
        }
        setErrors({});
      }
    } catch (error) {
      showToast(`Failed to load schema: ${schemaName}`, 'error');
      console.error('Error loading schema:', error);
    } finally {
      setLoading(false);
    }
  };

  const saveConfiguration = async () => {
    try {
      if (!selectedSchema) return;

      // Validate against schema
      const validationResponse = await configAPI.validateConfiguration(
        selectedSchema.name,
        formData
      );

      if (!validationResponse.valid) {
        const fieldErrors = {};
        validationResponse.errors?.forEach(error => {
          // Parse error message to extract field name
          const match = error.match(/^([^:]+):/);
          const field = match ? match[1] : 'general';
          fieldErrors[field] = error;
        });
        setErrors(fieldErrors);
        showToast('Please correct the validation errors', 'error');
        return;
      }

      // Save configuration via API (implementation would depend on your backend)
      console.log('Saving configuration:', {
        schema: selectedSchema.name,
        data: formData
      });

      showToast(`Configuration for ${selectedSchema.name} saved successfully`, 'success');
    } catch (error) {
      showToast('Failed to save configuration', 'error');
      console.error('Error saving configuration:', error);
    }
  };

  const saveProfile = async () => {
    const profileName = prompt('Enter profile name:');
    if (!profileName) return;

    try {
      const profile = {
        name: profileName,
        timestamp: new Date().toISOString(),
        configurations: formData
      };

      // Save to localStorage for now (could be extended to backend)
      const profiles = JSON.parse(localStorage.getItem('iped_profiles') || '[]');
      profiles.push(profile);
      localStorage.setItem('iped_profiles', JSON.stringify(profiles));

      showToast(`Profile "${profileName}" saved successfully`, 'success');
    } catch (error) {
      showToast('Failed to save profile', 'error');
    }
  };

  const loadProfile = () => {
    try {
      const profiles = JSON.parse(localStorage.getItem('iped_profiles') || '[]');
      if (profiles.length === 0) {
        showToast('No saved profiles', 'error');
        return;
      }

      const profileList = profiles.map((p, i) => `${i + 1}. ${p.name}`).join('\n');
      const choice = prompt(`Available profiles:\n${profileList}\n\nEnter profile number:`);

      if (!choice) return;

      const index = parseInt(choice) - 1;
      if (index < 0 || index >= profiles.length) {
        showToast('Invalid profile', 'error');
        return;
      }

      setFormData(profiles[index].configurations);
      showToast(`Profile "${profiles[index].name}" loaded successfully`, 'success');
    } catch (error) {
      showToast('Failed to load profile', 'error');
    }
  };

  const toggleCategory = (category) => {
    setExpandedCategories(prev => ({
      ...prev,
      [category]: !prev[category]
    }));
  };

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const groupedSchemas = schemas.reduce((acc, schema) => {
    const category = schema.category || 'Other';
    if (!acc[category]) acc[category] = [];
    acc[category].push(schema);
    return acc;
  }, {});

  return (
    <div className="app-container">
      {/* Header */}
      <header>
        <div className="header-left">
          <div className="header-title">Configurator</div>
          <div className="header-tabs">
            {['Workspace', 'Monitor', 'Logs'].map(tab => (
              <div
                key={tab}
                className={`header-tab ${activeTab === tab ? 'active' : ''}`}
                onClick={() => setActiveTab(tab)}
              >
                {tab}
              </div>
            ))}
          </div>
        </div>
        <div className="header-right">
          <button onClick={loadProfile}>⬇️ Load Profile</button>
          <button onClick={saveProfile}>💾 Save Profile</button>
          <button className="primary" onClick={saveConfiguration} disabled={!selectedSchema}>
            Save
          </button>
          <button className="icon-btn">⚙️</button>
        </div>
      </header>

      {/* Main Content */}
      <div className="main-container">
        {/* Sidebar */}
        <SchemaSelector
          groupedSchemas={groupedSchemas}
          selectedSchema={selectedSchema}
          expandedCategories={expandedCategories}
          onSelectSchema={loadSchema}
          onToggleCategory={toggleCategory}
          loading={loading}
        />

        {/* Content Area */}
        <div className="content-area">
          {selectedSchema ? (
            <>
              {/* Main Panel */}
              <ConfigurationPanel
                schema={selectedSchema}
                formData={formData}
                errors={errors}
                onFormDataChange={setFormData}
                onErrorsChange={setErrors}
              />

              {/* Info Panel */}
              <InfoPanel schema={selectedSchema} />
            </>
          ) : (
            <div className="empty-state">
              <div className="empty-state-icon">📋</div>
              <p>Select a component to begin</p>
            </div>
          )}
        </div>
      </div>

      {/* Toast Notification */}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}

export default App;
