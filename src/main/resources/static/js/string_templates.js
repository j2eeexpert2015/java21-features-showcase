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
  searchQuery: '',
  connected: false,
  processing: false
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

async function checkConnection() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/templates/demo-state`, {
      method: 'GET', headers: { 'Content-Type': 'application/json' }
    });

    if (response.ok) {
      appState.connected = true;
      updateConnectionStatus(true);
      const apiResponse = await response.json();
      if (apiResponse.metadata && apiResponse.metadata.demoState) {
        syncWithBackendState(apiResponse.metadata.demoState);
      }
      return true;
    }
  } catch (error) {
    console.log('Backend not available:', error.message);

    if (!appState.customerName) {
      document.getElementById('customerName').value = 'Sarah Johnson';
      document.getElementById('orderId').value = 'ORD-1001';
      document.getElementById('amount').value = '1299.99';
      document.getElementById('itemsCount').value = '3';
      document.getElementById('searchQuery').value = 'sarah.johnson@example.com';
    }
  }

  appState.connected = false;
  updateConnectionStatus(false);
  return false;
}

function updateConnectionStatus(connected) {
  const dot = document.getElementById('connectionDot');
  const text = document.getElementById('connectionText');

  if (connected) {
    dot.classList.add('connected');
    text.textContent = 'Backend Connected';
    document.querySelectorAll('button').forEach(btn => btn.disabled = false);
  } else {
    dot.classList.remove('connected');
    text.textContent = 'Backend Offline';
  }
}

function syncWithBackendState(backendState) {
  if (!backendState) return;

  if (backendState.customerName) {
    appState.customerName = backendState.customerName;
    document.getElementById('customerName').value = backendState.customerName;
  }
  if (backendState.orderId) {
    appState.orderId = backendState.orderId;
    document.getElementById('orderId').value = backendState.orderId;
  }
  if (backendState.amount) {
    appState.amount = parseFloat(backendState.amount);
    document.getElementById('amount').value = backendState.amount;
  }
  if (backendState.itemsCount) {
    appState.itemsCount = parseInt(backendState.itemsCount);
    document.getElementById('itemsCount').value = backendState.itemsCount;
  }
  if (backendState.searchQuery) {
    appState.searchQuery = backendState.searchQuery;
    document.getElementById('searchQuery').value = backendState.searchQuery;
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

  const statusIcon = appState.connected ? '🌐' : '⚠️';
  const statusText = appState.connected ? 'API Call' : 'Offline Mode';

  flowBlock.innerHTML = `
    <div>👤 <strong>${userAction}</strong> (Frontend)</div>
    <div class="api-flow-child">${statusIcon} ${statusText}: ${method} ${endpoint}</div>
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
  if (appState.processing) return;

  const emailBtn = document.getElementById('emailBtn');
  const outputSection = document.getElementById('template-output');
  const originalText = emailBtn.innerHTML;

  appState.processing = true;
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

    if (appState.connected) {
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
    } else {
      const logId = createFlowLog('Generate Email (Offline)', 'POST', '/api/templates/email');
      setTimeout(() => {
        updateFlowLog(logId, {
          controller_method: 'StringTemplateController.generateEmail (Simulated)',
          service_calls: { 'StringTemplateService.generateEmail': ['STR', 'Expression embedding'] },
          operation_description: 'Email template generated using STR processor (offline simulation)'
        });

        const emailContent = simulateEmailGeneration(templateRequest);
        displayTemplate(emailContent, 'email');
      }, 1000);
    }
  } catch (error) {
    console.error('Email generation failed:', error);
  } finally {
    appState.processing = false;
    emailBtn.disabled = false;
    emailBtn.classList.remove('processing');
    emailBtn.innerHTML = originalText;
    outputSection.classList.remove('processing');
  }
}

async function generateSMS() {
  if (appState.processing) return;

  const smsBtn = document.getElementById('smsBtn');
  const outputSection = document.getElementById('template-output');
  const originalText = smsBtn.innerHTML;

  appState.processing = true;
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

    if (appState.connected) {
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
    } else {
      const logId = createFlowLog('Generate SMS (Offline)', 'POST', '/api/templates/sms');
      setTimeout(() => {
        updateFlowLog(logId, {
          controller_method: 'StringTemplateController.generateSMS (Simulated)',
          service_calls: { 'StringTemplateService.generateSMS': ['FMT', 'Expression embedding', 'Formatted output'] },
          operation_description: 'SMS template generated using FMT processor (offline simulation)'
        });

        const smsContent = simulateSMSGeneration(templateRequest);
        displayTemplate(smsContent, 'sms');
      }, 1000);
    }
  } catch (error) {
    console.error('SMS generation failed:', error);
  } finally {
    appState.processing = false;
    smsBtn.disabled = false;
    smsBtn.classList.remove('processing');
    smsBtn.innerHTML = originalText;
    outputSection.classList.remove('processing');
  }
}

async function generateSQL() {
  if (appState.processing) return;

  const sqlBtn = document.getElementById('sqlBtn');
  const outputSection = document.getElementById('template-output');
  const originalText = sqlBtn.innerHTML;

  appState.processing = true;
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

    if (appState.connected) {
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
    } else {
      const logId = createFlowLog('Generate SQL (Offline)', 'POST', '/api/templates/sql');
      setTimeout(() => {
        updateFlowLog(logId, {
          controller_method: 'StringTemplateController.generateSQL (Simulated)',
          service_calls: { 'StringTemplateService.generateSQL': ['Custom', 'Expression embedding'] },
          operation_description: 'SQL template generated using custom processor (offline simulation)'
        });

        const sqlContent = simulateSQLGeneration(templateRequest);
        displayTemplate(sqlContent, 'sql');
      }, 1000);
    }
  } catch (error) {
    console.error('SQL generation failed:', error);
  } finally {
    appState.processing = false;
    sqlBtn.disabled = false;
    sqlBtn.classList.remove('processing');
    sqlBtn.innerHTML = originalText;
    outputSection.classList.remove('processing');
  }
}

function simulateEmailGeneration(data) {
  return {
    type: 'email',
    title: 'Order Confirmation Email',
    template: 'STR."Dear \\{customerName}, Your order #\\{orderId} has been confirmed!"',
    output: `Dear ${data.customerName},

Your order #${data.orderId} has been confirmed!

Order Details:
- Total Amount: ${data.amount.toFixed(2)}
- Items: ${data.itemsCount} product(s)
- Order Date: ${new Date().toLocaleDateString()}

Thank you for shopping with TechMart!

Best regards,
The TechMart Team`,
    processorUsed: 'STR Processor'
  };
}

function simulateSMSGeneration(data) {
  return {
    type: 'sms',
    title: 'SMS Notification',
    template: 'FMT."Order \\{orderId} confirmed! Total: \\{amount:$.2f} for \\{itemsCount} items"',
    output: `TechMart Alert
Order ${data.orderId} confirmed!
Total: ${data.amount.toFixed(2)} for ${data.itemsCount} items.
Track: techmart.com/track/${data.orderId}`,
    processorUsed: 'FMT Processor'
  };
}

function simulateSQLGeneration(data) {
  const sanitizedCustomerName = data.customerName ? data.customerName.replace(/[';"\-\-]/g, '').trim() : '';
  const sanitizedOrderId = data.orderId ? data.orderId.replace(/[';"\-\-]/g, '').trim() : '';

  return {
    type: 'sql',
    title: 'Database Query',
    template: 'SAFE."SELECT * FROM orders WHERE customer_name = \\{customerName} AND order_id = \\{orderId} AND total_amount >= \\{amount} AND item_count <= \\{itemsCount}"',
    output: `-- Generated Safe SQL Query (Demo)
-- All inputs sanitized: dangerous characters removed

SELECT o.order_id, o.customer_name, o.total_amount, o.item_count, o.status, o.order_date
FROM orders o
WHERE o.customer_name = '${sanitizedCustomerName}'
  AND o.order_id = '${sanitizedOrderId}'
  AND o.total_amount >= ${data.amount}
  AND o.item_count <= ${data.itemsCount}
  AND o.status = 'active'
  AND o.order_date >= CURRENT_DATE - INTERVAL 365 DAY
ORDER BY o.order_date DESC;

-- Security Note: In production, use PreparedStatement with ? placeholders
-- This demo shows input sanitization as a String Template security example`,
    processorUsed: 'Custom Safe Processor'
  };
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

async function initApp() {
  const logId = createFlowLog('App Initialization', 'GET', '/api/templates/demo-state');
  const connected = await checkConnection();

  if (connected) {
    updateFlowLog(logId, {
      controller_method: 'StringTemplateController.getDemoState',
      service_calls: {},
      operation_description: 'Backend connected - Demo ready'
    });
  } else {
    updateFlowLog(logId, {
      controller_method: 'Frontend Only Mode',
      service_calls: {},
      operation_description: 'Backend offline - Running in simulation mode'
    });
  }
}

document.addEventListener('DOMContentLoaded', initApp);
setInterval(checkConnection, 30000);
