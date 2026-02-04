const API_BASE_URL = 'http://localhost:8080';
const ENDPOINTS = {
  EMAIL: '/api/templates/email',
  SMS: '/api/templates/sms',
  SQL: '/api/templates/sql',
  DEMO_STATE: '/api/templates/demo-state',
  RESET: '/api/templates/reset'
};

let appState = {
  customerName: '',
  orderId: '',
  amount: 0,
  itemsCount: 0,
  searchQuery: ''
};

async function apiCall(method, endpoint, data = null) {
  const logId = createFlowLog(`${method.toUpperCase()} Request`, method.toUpperCase(), endpoint);

  try {
    const config = { method: method.toUpperCase(), headers: { 'Content-Type': 'application/json' } };
    if (data && (method.toUpperCase() === 'POST' || method.toUpperCase() === 'PUT')) {
      config.body = JSON.stringify(data);
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, config);
    if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);

    const responseData = await response.json();

    updateFlowLog(logId, {
      controller_method: responseData.controllerMethod || 'StringTemplateController',
      service_calls: responseData.serviceCalls || {},
      operation_description: responseData.operationDescription || 'Operation completed',
      response_data: responseData
    });

    return responseData;
  } catch (error) {
    console.error('API Call failed:', error);
    updateFlowLog(logId, { error: error.message, controller_method: 'Connection Failed' });
    throw error;
  }
}

function createFlowLog(userAction, method, endpoint) {
  const logContainer = document.getElementById('api-log');
  if (!logContainer) return null;

  const initialMessages = logContainer.querySelectorAll('.text-muted, .text-center');
  initialMessages.forEach(msg => {
    if (msg.textContent && (msg.textContent.includes('Starting') || msg.textContent.includes('Click'))) {
      logContainer.innerHTML = '';
    }
  });

  const flowBlock = document.createElement('div');
  const logId = `flow-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  flowBlock.id = logId;
  flowBlock.className = 'api-flow-block';

  flowBlock.innerHTML = `
    <div>👤 <strong>${userAction}</strong> (Frontend)</div>
    <div class="api-flow-child">🌐 API Call: ${method} ${endpoint}</div>
    <div class="api-flow-child" data-role="controller"><div class="spinner"></div> Processing...</div>
  `;

  logContainer.insertBefore(flowBlock, logContainer.firstChild);

  while (logContainer.children.length > 12) {
    logContainer.removeChild(logContainer.lastChild);
  }

  logContainer.scrollTop = 0;
  return logId;
}

function updateFlowLog(logId, responseData) {
  if (!logId) return;

  const flowBlock = document.getElementById(logId);
  if (!flowBlock) return;

  const controllerElement = flowBlock.querySelector('[data-role="controller"]');
  if (!controllerElement) return;

  if (responseData.error) {
    flowBlock.classList.add('error');
    controllerElement.innerHTML = `🔴 <span class="error-text">Error: ${responseData.error}</span>`;
    return;
  }

  flowBlock.classList.add('success');
  let html = `🔴 Controller: <strong>${responseData.controller_method || 'StringTemplateController'}</strong>`;

  if (responseData.service_calls && Object.keys(responseData.service_calls).length > 0) {
    html += `<div class="api-flow-child">🟣 Service Layer: Template processing executed</div>`;

    Object.entries(responseData.service_calls).forEach(([serviceMethod, templateFeatures]) => {
      if (Array.isArray(templateFeatures) && templateFeatures.length > 0) {
        html += `<div class="api-flow-child">  └── <strong>${serviceMethod}</strong> → <span class="java21-method-tag">${templateFeatures.join(', ')}</span></div>`;
        templateFeatures.forEach(method => highlightJavaMethod(method));
      } else {
        html += `<div class="api-flow-child">  └── <strong>${serviceMethod}</strong> → Standard processing</div>`;
      }
    });
  }

  if (responseData.operation_description) {
    html += `<div class="api-flow-child">💡 <span class="success-text">${responseData.operation_description}</span></div>`;
  }

  if (responseData.templateResponse || responseData.response_data?.templateResponse || responseData.metadata?.templateResponse) {
    const template = responseData.templateResponse || responseData.response_data?.templateResponse || responseData.metadata?.templateResponse;
    html += `<div class="api-flow-child">📄 Template: <strong>${template.templateType}</strong> (${template.processorUsed})</div>`;
  }

  controllerElement.innerHTML = html;
}

function clearInspectorLog() {
  const logContainer = document.getElementById('api-log');
  if (!logContainer) return;
  logContainer.innerHTML = '<div class="text-muted text-center py-2">Log cleared. Generate templates to see API calls...</div>';
}

function highlightJavaMethod(methodName) {
  if (!methodName) return;

  const safe = String(methodName).replace(/\(\)$/, '');

  document.querySelectorAll('.api-reference-table tr[data-highlight="1"]').forEach(row => {
    const cells = Array.from(row.cells);
    cells.forEach(cell => {
      cell.style.removeProperty('box-shadow');
      cell.style.removeProperty('background-color');
      cell.style.removeProperty('border-top');
      cell.style.removeProperty('border-bottom');
      cell.style.removeProperty('border-left');
      cell.style.removeProperty('border-right');
    });
    row.removeAttribute('data-highlight');
  });

  let targetSelector = null;
  switch (safe.toLowerCase()) {
    case 'str':
    case 'string template':
    case 'safe string interpolation':
      targetSelector = '#code-STR'; break;
    case 'fmt':
    case 'formatted output':
    case 'format':
      targetSelector = '#code-FMT'; break;
    case 'custom':
    case 'custom template processors':
      targetSelector = '#code-custom'; break;
    case 'expression embedding':
    case 'expression':
      targetSelector = '#code-expression'; break;
  }

  if (targetSelector) {
    const row = document.querySelector(targetSelector);
    if (row) {
      const cells = Array.from(row.cells);
      const bg = '#fffbea';
      const border = '#ffc107';

      cells.forEach((cell, i) => {
        cell.style.setProperty('box-shadow', `inset 0 0 0 9999px ${bg}`, 'important');
        cell.style.setProperty('background-color', 'transparent', 'important');
        cell.style.setProperty('border-top', `2px solid ${border}`, 'important');
        cell.style.setProperty('border-bottom', `2px solid ${border}`, 'important');
        if (i === 0) cell.style.setProperty('border-left', `2px solid ${border}`, 'important');
        if (i === cells.length - 1) cell.style.setProperty('border-right', `2px solid ${border}`, 'important');
      });

      row.setAttribute('data-highlight', '1');

      setTimeout(() => {
        if (row.isConnected) {
          cells.forEach(cell => {
            cell.style.removeProperty('box-shadow');
            cell.style.removeProperty('background-color');
            cell.style.removeProperty('border-top');
            cell.style.removeProperty('border-bottom');
            cell.style.removeProperty('border-left');
            cell.style.removeProperty('border-right');
          });
          row.removeAttribute('data-highlight');
        }
      }, 3000);
    }
  }
}

async function generateEmail() {
  const emailBtn = document.getElementById('emailBtn');
  const outputSection = document.getElementById('template-output');
  const originalText = emailBtn.innerHTML;

  emailBtn.disabled = true;
  emailBtn.classList.add('processing');
  emailBtn.innerHTML = '<div class="spinner"></div> Processing...';
  outputSection.classList.add('processing');

  try {
    const templateRequest = {
      customerName: document.getElementById('customerName').value,
      orderId: document.getElementById('orderId').value,
      amount: parseFloat(document.getElementById('amount').value),
      itemsCount: parseInt(document.getElementById('itemsCount').value),
      searchQuery: document.getElementById('searchQuery').value
    };

    const response = await apiCall('POST', ENDPOINTS.EMAIL, templateRequest);
    
    if (response.templateResponse || response.response_data?.templateResponse || response.metadata?.templateResponse) {
      const templateData = response.templateResponse || response.response_data?.templateResponse || response.metadata?.templateResponse;
      displayTemplate({
        type: 'email',
        title: 'Order Confirmation Email',
        template: templateData.templateSource || 'STR template processed',
        output: templateData.generatedContent,
        processorUsed: templateData.processorUsed || 'STR Processor'
      }, 'email');
    }
  } catch (error) {
    console.error('Email generation failed:', error);
    showError('Failed to generate email. Please ensure the backend is running.');
  } finally {
    emailBtn.disabled = false;
    emailBtn.classList.remove('processing');
    emailBtn.innerHTML = originalText;
    outputSection.classList.remove('processing');
  }
}

async function generateSMS() {
  const smsBtn = document.getElementById('smsBtn');
  const outputSection = document.getElementById('template-output');
  const originalText = smsBtn.innerHTML;

  smsBtn.disabled = true;
  smsBtn.classList.add('processing');
  smsBtn.innerHTML = '<div class="spinner"></div> Processing...';
  outputSection.classList.add('processing');

  try {
    const templateRequest = {
      customerName: document.getElementById('customerName').value,
      orderId: document.getElementById('orderId').value,
      amount: parseFloat(document.getElementById('amount').value),
      itemsCount: parseInt(document.getElementById('itemsCount').value)
    };

    const response = await apiCall('POST', ENDPOINTS.SMS, templateRequest);
    
    if (response.templateResponse || response.response_data?.templateResponse || response.metadata?.templateResponse) {
      const templateData = response.templateResponse || response.response_data?.templateResponse || response.metadata?.templateResponse;
      displayTemplate({
        type: 'sms',
        title: 'SMS Notification',
        template: templateData.templateSource || 'FMT template processed',
        output: templateData.generatedContent,
        processorUsed: templateData.processorUsed || 'FMT Processor'
      }, 'sms');
    }
  } catch (error) {
    console.error('SMS generation failed:', error);
    showError('Failed to generate SMS. Please ensure the backend is running.');
  } finally {
    smsBtn.disabled = false;
    smsBtn.classList.remove('processing');
    smsBtn.innerHTML = originalText;
    outputSection.classList.remove('processing');
  }
}

async function generateSQL() {
  const sqlBtn = document.getElementById('sqlBtn');
  const outputSection = document.getElementById('template-output');
  const originalText = sqlBtn.innerHTML;

  sqlBtn.disabled = true;
  sqlBtn.classList.add('processing');
  sqlBtn.innerHTML = '<div class="spinner"></div> Processing...';
  outputSection.classList.add('processing');

  try {
    const templateRequest = {
      customerName: document.getElementById('customerName').value,
      orderId: document.getElementById('orderId').value,
      amount: parseFloat(document.getElementById('amount').value),
      itemsCount: parseInt(document.getElementById('itemsCount').value),
      searchQuery: document.getElementById('searchQuery').value
    };

    const response = await apiCall('POST', ENDPOINTS.SQL, templateRequest);
    
    if (response.templateResponse || response.response_data?.templateResponse || response.metadata?.templateResponse) {
      const templateData = response.templateResponse || response.response_data?.templateResponse || response.metadata?.templateResponse;
      displayTemplate({
        type: 'sql',
        title: 'Database Query',
        template: templateData.templateSource || 'SQL template processed',
        output: templateData.generatedContent,
        processorUsed: templateData.processorUsed || 'Custom Processor'
      }, 'sql');
    }
  } catch (error) {
    console.error('SQL generation failed:', error);
    showError('Failed to generate SQL. Please ensure the backend is running.');
  } finally {
    sqlBtn.disabled = false;
    sqlBtn.classList.remove('processing');
    sqlBtn.innerHTML = originalText;
    outputSection.classList.remove('processing');
  }
}

function showError(message) {
  const outputContainer = document.getElementById('template-output');
  outputContainer.innerHTML = `
    <div class="alert alert-danger" role="alert">
      <i class="fas fa-exclamation-circle"></i> <strong>Error:</strong> ${message}
    </div>
  `;
}

function displayTemplate(content, type) {
  const outputContainer = document.getElementById('template-output');

  let typeIcon = '';
  let typeClass = '';
  switch (type) {
    case 'email': typeIcon = '📧'; typeClass = 'email-preview'; break;
    case 'sms':   typeIcon = '📱'; typeClass = 'sms-preview';   break;
    case 'sql':   typeIcon = '🗄️'; typeClass = 'sql-preview';   break;
  }

  outputContainer.innerHTML = `
    <div class="template-preview ${typeClass}">
      <div class="d-flex justify-content-between align-items-center mb-2">
        <h6>${typeIcon} ${content.title}</h6>
      </div>

      <div class="mb-2">
        <small class="text-muted">Template:</small><br>
        <code style="background:#f8f9fa; padding:.25rem; border-radius:.25rem; font-size:.8rem;">${content.template}</code>
      </div>

      <div class="mb-2">
        <small class="text-muted">Generated Output:</small>
        <div style="background:white; padding:.75rem; border:1px solid #e9ecef; border-radius:.25rem; margin-top:.25rem;">
          <pre style="margin:0; white-space:pre-wrap; font-family:inherit; font-size:.9rem;">${content.output}</pre>
        </div>
      </div>

      <div style="font-size:.8rem; color:#6c757d;">
        <strong>Processor Used:</strong> ${content.processorUsed}
      </div>
    </div>
  `;
}

function initApp() {
  // Set default values
  document.getElementById('customerName').value = 'Sarah Johnson';
  document.getElementById('orderId').value = 'ORD-1001';
  document.getElementById('amount').value = '1299.99';
  document.getElementById('itemsCount').value = '3';
  document.getElementById('searchQuery').value = 'sarah.johnson@example.com';
  
  console.log('TechMart String Templates Demo initialized');
}

document.addEventListener('DOMContentLoaded', initApp);
