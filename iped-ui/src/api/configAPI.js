import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

export const configAPI = {
  // Schema endpoints
  listSchemas: async () => {
    try {
      const response = await api.get('/schemas');
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message,
        schemas: []
      };
    }
  },

  getSchema: async (componentName) => {
    try {
      const response = await api.get(`/schemas/${componentName}`);
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  },

  getUISchema: async (componentName) => {
    try {
      const response = await api.get(`/schemas/${componentName}/ui`);
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  },

  validateConfiguration: async (componentName, configData) => {
    try {
      const response = await api.post(`/schemas/${componentName}/validate`, configData);
      return response.data;
    } catch (error) {
      return {
        success: false,
        valid: false,
        errors: [error.message]
      };
    }
  },

  getSchemasByCategory: async (category) => {
    try {
      const response = await api.get(`/schemas/category/${category}`);
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message,
        schemas: []
      };
    }
  },

  // Configuration endpoints
  getCurrentConfigurations: async () => {
    try {
      const response = await api.get('/configurations');
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message,
        configurations: {}
      };
    }
  },

  getConfiguration: async (componentName) => {
    try {
      const response = await api.get(`/configurations/${componentName}`);
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message,
        data: {}
      };
    }
  },

  saveConfiguration: async (componentName, configData) => {
    try {
      const response = await api.post(`/configurations/${componentName}`, configData);
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  },

  exportConfiguration: async (componentName) => {
    try {
      const response = await api.get(`/configurations/${componentName}/export`);
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  },

  exportAllConfigurations: async () => {
    try {
      const response = await api.get('/configurations/export/all');
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  },

  getConfigurationMetadata: async () => {
    try {
      const response = await api.get('/configurations/metadata');
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  },

  createBackup: async (backupPath) => {
    try {
      const response = await api.post('/configurations/backup', { path: backupPath });
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  },

  // Diff and Merge endpoints
  diffConfigurations: async (config1, config2) => {
    try {
      const response = await api.post('/configurations/diff', { config1, config2 });
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message
      };
    }
  },

  mergeConfigurations: async (baseConfig, config1, config2) => {
    try {
      const response = await api.post('/configurations/merge', {
        baseConfig,
        config1,
        config2
      });
      return response.data;
    } catch (error) {
      return {
        success: false,
        error: error.message,
        conflicts: []
      };
    }
  }
};
