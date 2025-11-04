/*
* Corrected payment_processing.js
*
* FIX 1: (Instant Update)
* - Removed updatePatternHighlighting() and updateStatusIndicators() from input handlers.
* - Added resetStatusPanels() to clear panels on init, clear, and process.
* - processPayment() now calls updateStatusIndicators() *after* API call.
*
* FIX 2: (Highlighting Bug)
* - REMOVED highlight clearing from highlightJavaMethod().
* - ADDED highlight clearing to the TOP of renderPatternMatchingSteps().
* - REMOVED 3-second timeout from highlightJavaMethod() to make highlights persist.
*/

const API_BASE_URL = 'http://localhost:8080';
const ENDPOINTS = {
  PROCESS: '/api/payment/process',
  METHOD: '/api/payment/method',
  CUSTOMER_TYPE: '/api/payment/customer/type',
  INTERNATIONAL: '/api/payment/international',
  AMOUNT: '/api/payment/amount',
  DEMO_STATE: '/api/payment/demo-state'
};

let appState = {
  amount: 500,
  paymentMethod: 'credit',
  customerType: 'vip',
  isInternational: false,
  processing: false,
  connected: false
};

const scenarios = {
  500: {
    items: [
      { name: 'AirPods Pro', price: 249 },
      { name: 'Apple Watch Band', price: 251 }
    ],
    total: 500
  },
  1500: {
    items: [
      { name: 'iPhone 15', price: 799 },
      { name: 'AirPods Pro', price: 249 },
      { name: 'AppleCare+ & Accessories', price: 452 }
    ],
    total: 1500
  },
  5000: {
    items: [
      { name: 'iPhone 15 Pro Max', price: 1199 },
      { name: 'MacBook Air M2', price: 1199 },
      { name: 'iPad Pro 12.9"', price: 1099 },
      { name: 'Apple Watch Ultra', price: 799 },
      { name: 'AirPods Max', price: 704 }
    ],
    total: 5000
  }
};

const paymentMethods = {
  credit: {
    name: 'CreditCard',
    pattern: 'CreditCard(var number, var type, var cvv)',
    validation: 'CVV + fraud detection',
    processingTime: 'Instant'
  },
  paypal: {
    name: 'PayPal',
    pattern: 'PayPal(var email, var accountId)',
    validation: 'Account verification',
    processingTime: '1-2 minutes'
  },
  bank: {
    name: 'BankTransfer',
    pattern: 'BankTransfer(var routing, var account)',
    validation: 'Bank verification',
    processingTime: '2-3 business days'
  }
};

const customerTypes = {
  basic: { tier: 'Basic', priority: 'Standard' },
  premium: { tier: 'Premium', priority: 'High' },
  vip: { tier: 'VIP', priority: 'Express' }
};

// Generate mock pattern matching steps for offline mode
function generateMockPatternSteps() {
  const steps = [];
  const method = paymentMethods[appState.paymentMethod];

  steps.push({
    stepType: 'TYPE_CHECK',
    description: `Payment is ${method.name}`,
    matchedType: method.name,
    timestamp: new Date().toISOString()
  });

  const destructuredValues = appState.paymentMethod === 'credit'
    ? `type=Visa, cvv=***`
    : appState.paymentMethod === 'paypal'
      ? `email=user@example.com, accountId=PP-${Date.now().toString().slice(-6)}`
      : `routing=021000021, account=****1234`;

  steps.push({
    stepType: 'DESTRUCTURING',
    description: `Record pattern destructured ${method.name} components`,
    matchedType: method.name,
    extractedValues: destructuredValues,
    timestamp: new Date().toISOString()
  });

  if (appState.amount > 1000 && appState.isInternational) {
    steps.push({
      stepType: 'GUARD_EVALUATION',
      description: `Guard condition for high-value international transactions`,
      guardExpression: 'amount > 1000 && international',
      passed: true,
      caseNumber: 1,
      evaluationDetails: [
        { condition: 'amount > 1000', result: true, actualValue: `$${appState.amount.toLocaleString()} > $1,000` },
        { condition: 'international', result: true, actualValue: 'TRUE' }
      ],
      timestamp: new Date().toISOString()
    });
  } else if (appState.amount > 1000 && !appState.isInternational) {
    steps.push({
      stepType: 'GUARD_EVALUATION',
      description: `Checking guard for high-value international case`,
      guardExpression: 'amount > 1000 && international',
      passed: false,
      caseNumber: 1,
      evaluationDetails: [
        { condition: 'amount > 1000', result: true, actualValue: `$${appState.amount.toLocaleString()} > $1,000` },
        { condition: 'international', result: false, actualValue: 'FALSE' }
      ],
      timestamp: new Date().toISOString()
    });
    steps.push({
      stepType: 'GUARD_EVALUATION',
      description: `Guard condition for high-value domestic transactions`,
      guardExpression: 'amount > 1000',
      passed: true,
      caseNumber: 2,
      evaluationDetails: [
        { condition: 'amount > 1000', result: true, actualValue: `$${appState.amount.toLocaleString()} > $1,000` }
      ],
      timestamp: new Date().toISOString()
    });
  }

  return steps;
}

async function apiCall(method, endpoint, data = null) {
  const logId = createFlowLog(
    `${method.toUpperCase()} Request`,
    method.toUpperCase(),
    endpoint
  );

  try {
    const config = {
      method: method.toUpperCase(),
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include'
    };
    if (data && (method.toUpperCase() === 'POST' || method.toUpperCase() === 'PUT')) {
      config.body = JSON.stringify(data);
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, config);
    if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);

    const responseData = await response.json();

    updateFlowLog(logId, {
      controller_method: responseData.controller_method || 'PaymentController',
      service_calls: responseData.service_calls || {},
      operation_description: responseData.operation_description || 'Operation completed',
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
    const response = await fetch(`${API_BASE_URL}/api/payment/demo-state`, {
      method: 'GET', headers: { 'Content-Type': 'application/json' }, credentials: 'include'
    });

    if (response.ok) {
      appState.connected = true;
      updateConnectionStatus(true);
      const demoState = await response.json();
      syncWithBackendState(demoState);
      return true;
    }
  } catch (error) {
    console.log('Backend not available:', error.message);
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

  if (backendState.amount) {
    appState.amount = parseFloat(backendState.amount);
    updateAmountDisplays(appState.amount);
  }
  if (backendState.paymentMethod) {
    appState.paymentMethod = backendState.paymentMethod;
    updatePaymentMethodUI(backendState.paymentMethod);
  }
  if (backendState.customerType) {
    appState.customerType = backendState.customerType.toLowerCase();
    updateCustomerTypeUI(appState.customerType);
  }
  if (typeof backendState.international === 'boolean') {
    appState.isInternational = backendState.international;
    document.getElementById('international').checked = backendState.international;
  }

  // **FIX 1:** Do NOT call update functions here
  // updateStatusIndicators();
  // updatePatternHighlighting();
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

function renderPatternMatchingSteps(steps) {
  if (!steps || steps.length === 0) return '';

  // **FIX 2 (Highlighting):** Clear highlights ONCE before the loop begins.
  document.querySelectorAll('.pattern-line.highlighted').forEach(line => line.classList.remove('highlighted'));

  let html = '<div class="pm-execution-header">🟣 Pattern Matching Execution:</div>';

  steps.forEach((step, index) => {
    html += `<div class="pm-step">`;
    const stepNum = step.number || (index + 1);
    const stepType = step.type || step.stepType;
    const stepMessage = step.message || step.description;
    const isPassed = step.passed !== undefined ? step.passed : true;

    html += `<div class="pm-step-header">📍 Step ${stepNum}: ${stepType.replace(/_/g, ' ')}</div>`;

    if (stepType === 'TYPE_CHECK') {
      html += `<div class="pm-step-detail success">✓ ${stepMessage}</div>`;
      highlightJavaMethod('switch');
      if (stepMessage.includes('CreditCard')) highlightJavaMethod('credit');
      else if (stepMessage.includes('PayPal')) highlightJavaMethod('paypal');
      else if (stepMessage.includes('BankTransfer')) highlightJavaMethod('bank');
    } else if (stepType === 'DESTRUCTURING') {
      html += `<div class="pm-step-detail success">✓ ${stepMessage}</div>`;
      if (stepMessage.includes('CreditCard')) highlightJavaMethod('credit');
      else if (stepMessage.includes('PayPal')) highlightJavaMethod('paypal');
      else if (stepMessage.includes('BankTransfer')) highlightJavaMethod('bank');
    } else if (stepType === 'GUARD_EVALUATION') {
      const guardExpr = step.guardExpression || '';
      html += `<div class="pm-step-detail">🔍 Checking: ${guardExpr}</div>`;
      const conditions = step.conditions || step.evaluationDetails || [];

      if (conditions.length > 0) {
        conditions.forEach(detail => {
          const conditionName = detail.name || detail.condition;
          const conditionPassed = detail.passed !== undefined ? detail.passed : detail.result;
          const conditionValue = detail.result || detail.actualValue || '';
          const icon = conditionPassed ? '✓' : '✗';
          const cssClass = conditionPassed ? 'passed' : 'failed';
          html += `<div class="pm-guard-check ${cssClass}">   ${icon} ${conditionName}: ${conditionPassed ? 'TRUE' : 'FALSE'} (${conditionValue})</div>`;
        });
      }

      const resultClass = isPassed ? 'passed' : 'failed';
      const resultIcon = isPassed ? '✅' : '⚠️';
      html += `<div class="pm-result ${resultClass}">${resultIcon} ${stepMessage}</div>`;
      if (isPassed) highlightJavaMethod('guard');
    }

    html += `</div>`;
  });

  return html;
}

function updateFlowLog(logId, responseData) {
  if (!logId) return;
  const flowBlock = document.getElementById(logId);
  if (!flowBlock) return;

  const controllerElement = flowBlock.querySelector('[data-role="controller"]');
  if (!controllerElement) return;

  const actualData = responseData.response_data || responseData;
  const controllerMethod = actualData.controllerMethod || responseData.controller_method || 'PaymentController';
  const operationDesc = actualData.operationDescription || responseData.operation_description || 'Operation completed';

  if (responseData.error || actualData.error) {
    flowBlock.classList.add('error');
    controllerElement.innerHTML = `🔴 <span class="error-text">Error: ${responseData.error || actualData.error}</span>`;
    return;
  }

  flowBlock.classList.add('success');
  let html = `🔴 Controller: <strong>${controllerMethod}</strong>`;

  let pmSteps = null;
  // **FIX:** Added better logic to find the nested paymentResponse
  if (actualData?.metadata?.paymentResponse?.patternMatchingSteps) {
    pmSteps = actualData.metadata.paymentResponse.patternMatchingSteps;
  } else if (actualData?.paymentResponse?.patternMatchingSteps) {
    pmSteps = actualData.paymentResponse.patternMatchingSteps;
  } else if (actualData?.patternMatchingSteps) {
    pmSteps = actualData.patternMatchingSteps;
  }

  if (pmSteps && pmSteps.length > 0) {
    // This function WILL call highlightJavaMethod
    html += renderPatternMatchingSteps(pmSteps);
  }

  if (operationDesc) {
    html += `<div class="api-flow-child">💡 <span class="success-text">${operationDesc}</span></div>`;
  }

  // **FIX:** Added better logic to find the nested paymentResponse
  let txnData = actualData?.metadata?.paymentResponse || actualData?.paymentResponse || actualData;
  if (txnData.transactionId) {
    html += `<div class="api-flow-child">💳 Transaction: <strong>${txnData.transactionId}</strong> (${txnData.status})</div>`;
    if (txnData.validationMessage) {
      html += `<div class="api-flow-child">📝 Result: ${txnData.validationMessage}</div>`;
    }
  }

  controllerElement.innerHTML = html;
}

function clearInspectorLog() {
  const logContainer = document.getElementById('api-log');
  if (!logContainer) return;
  logContainer.innerHTML = '<div class="text-muted text-center py-2">Log cleared. Perform actions to see API calls...</div>';

  // **FIX 1:** Also reset the middle panel
  resetStatusPanels();
}

// **FIX 1:** ***NEW FUNCTION***
// Resets the middle panel to its default state
function resetStatusPanels() {
    // 1. Reset Pattern Matching Reference highlights
    document.querySelectorAll('.pattern-line.highlighted').forEach(line => line.classList.remove('highlighted'));

    // 2. Reset Current Pattern Status text
    document.getElementById('payment-detection').textContent = 'Pattern: Awaiting processing...';

    const guardIcon = document.querySelector('#status-section .status-item:nth-child(2) .status-icon');
    guardIcon.className = 'status-icon pending';
    guardIcon.textContent = '○';
    document.getElementById('guard-condition').textContent = 'Guard: Awaiting processing...';

    const validationIcon = document.querySelector('#status-section .status-item:nth-child(3) .status-icon');
    validationIcon.className = 'status-icon success';
    validationIcon.textContent = '✓';
    document.getElementById('validation-status').textContent = 'Ready for processing';

    const processingIcon = document.querySelector('#status-section .status-item:nth-child(4) .status-icon');
    processingIcon.className = 'status-icon play';
    processingIcon.textContent = '▶';
    document.getElementById('processing-status').textContent = 'Click Process to execute';
}


function highlightJavaMethod(methodName) {
  if (!methodName) return;
  const safe = String(methodName).replace(/\(\)$/, '');

  // **FIX 2 (Highlighting):** The line clearing highlights has been REMOVED from here.
  // document.querySelectorAll('.pattern-line.highlighted').forEach(line => line.classList.remove('highlighted'));

  let targetSelector = null;
  switch (safe.toLowerCase()) {
    case 'switch':
    case 'pattern matching': targetSelector = '[data-pattern="switch"]'; break;
    case 'creditcard':
    case 'credit': targetSelector = '[data-pattern="credit"]'; break;
    case 'paypal': targetSelector = '[data-pattern="paypal"]'; break;
    case 'banktransfer':
    case 'bank': targetSelector = '[data-pattern="bank"]'; break;
    case 'guard':
    case 'when': targetSelector = '[data-pattern="guard"]'; break;
    case 'sealed': targetSelector = '[data-pattern="sealed"]'; break;
  }

  if (targetSelector) {
    const line = document.querySelector(targetSelector);
    if (line) {
      line.classList.add('highlighted');
      // **FIX:** Removed the timeout so the highlight persists
      // setTimeout(() => { if (line.isConnected) line.classList.remove('highlighted'); }, 3000);
    }
  }
}

function updateAmountDisplays(amount) {
  document.getElementById('total-amount').textContent = `$${amount.toLocaleString()}`;
  document.getElementById('btn-amount').textContent = `$${amount.toLocaleString()}`;
}

function updatePaymentMethodUI(method) {
  document.querySelectorAll('.payment-method').forEach(pm => pm.classList.remove('selected'));
  const methodElement = document.querySelector(`[data-method="${method}"]`);
  if (methodElement) methodElement.classList.add('selected');
}

function updateCustomerTypeUI(type) {
  document.querySelectorAll('.customer-tab').forEach(tab => tab.classList.remove('active'));
  const typeElement = document.querySelector(`[data-type="${type}"]`);
  if (typeElement) typeElement.classList.add('active');
}

function updateOrderDisplay(scenario) {
  const orderItems = document.getElementById('order-items');
  orderItems.innerHTML = scenario.items
    .map(item => `<div class="order-item"><span>${item.name}</span><span>$${item.price.toLocaleString()}</span></div>`)
    .join('');
}

async function setQuickTest(amount) {
  document.querySelectorAll('.test-btn').forEach(btn => btn.classList.remove('active'));
  if (typeof event !== 'undefined' && event.target) event.target.classList.add('active');

  appState.amount = amount;
  updateOrderDisplay(scenarios[amount]);
  updateAmountDisplays(amount);

  // **FIX 1:** Removed calls to updatePatternHighlighting() and updateStatusIndicators()
  // updatePatternHighlighting();
  // updateStatusIndicators();

  if (appState.connected) {
    try { await apiCall('PUT', `${ENDPOINTS.AMOUNT}/${amount}`); }
    catch (error) { console.error('Failed to update amount on backend:', error); }
  }
}

async function selectPaymentMethod(method) {
  document.querySelectorAll('.payment-method').forEach(pm => pm.classList.remove('selected'));
  if (typeof event !== 'undefined' && event.target) {
    const card = event.target.closest('.payment-method');
    if (card) card.classList.add('selected');
  }

  appState.paymentMethod = method;

  // **FIX 1:** Removed calls to updatePatternHighlighting() and updateStatusIndicators()
  // updatePatternHighlighting();
  // updateStatusIndicators();

  if (appState.connected) {
    try { await apiCall('PUT', `${ENDPOINTS.METHOD}/${method}`); }
    catch (error) { console.error('Failed to update payment method on backend:', error); }
  }
}

async function selectCustomerType(type) {
  document.querySelectorAll('.customer-tab').forEach(tab => tab.classList.remove('active'));
  if (typeof event !== 'undefined' && event.target) event.target.classList.add('active');

  appState.customerType = type;

  // **FIX 1:** Removed call to updateStatusIndicators()
  // updateStatusIndicators();

  if (appState.connected) {
    try { await apiCall('PUT', `${ENDPOINTS.CUSTOMER_TYPE}/${type.toUpperCase()}`); }
    catch (error) { console.error('Failed to update customer type on backend:', error); }
  }
}

async function toggleInternational() {
  appState.isInternational = document.getElementById('international').checked;

  // **FIX 1:** Removed call to updateStatusIndicators()
  // updateStatusIndicators();

  if (appState.connected) {
    try { await apiCall('PUT', `${ENDPOINTS.INTERNATIONAL}/${appState.isInternational}`); }
    catch (error) { console.error('Failed to update international status on backend:', error); }
  }
}

async function processPayment() {
  if (appState.processing) return;

  appState.processing = true;
  const processBtn = document.getElementById('processBtn');
  const originalText = processBtn.innerHTML;
  processBtn.disabled = true;
  processBtn.classList.add('processing');
  processBtn.innerHTML = '<div class="spinner"></div> Processing...';

  // **FIX 1:** Reset panels at the START of processing
  resetStatusPanels();

  try {
    if (appState.connected) {
      const paymentRequest = {
        customerId: 1,
        paymentMethod: appState.paymentMethod,
        amount: appState.amount,
        customerType: appState.customerType.toUpperCase(),
        international: appState.isInternational
      };

      const response = await apiCall('POST', ENDPOINTS.PROCESS, paymentRequest);

      // **FIX 1:** Update status panel AFTER API call
      updateStatusIndicators(); // This updates the status text

      // This logic handles the nested response structure
      let paymentResponseData = null;
      if (response && response.response_data && response.response_data.metadata && response.response_data.metadata.paymentResponse) {
          paymentResponseData = response.response_data.metadata.paymentResponse;
      } else if (response && response.response_data && response.response_data.paymentResponse) {
          paymentResponseData = response.response_data.paymentResponse;
      } else if (response && response.metadata && response.metadata.paymentResponse) {
          paymentResponseData = response.metadata.paymentResponse;
      } else if (response && response.paymentResponse) {
          paymentResponseData = response.paymentResponse;
      }

      if (paymentResponseData) {
          updateProcessingResults(paymentResponseData); // This adds final transaction ID
      }

    } else {
      const logId = createFlowLog('Process Payment (Offline)', 'POST', '/api/payment/process');
      const mockSteps = generateMockPatternSteps();

      setTimeout(() => {
        updateFlowLog(logId, {
          controller_method: 'PaymentController.processPayment (Simulated)',
          operation_description: `Payment processed successfully (offline simulation)`,
          response_data: {
            paymentResponse: {
              transactionId: 'TXN-OFFLINE-' + Date.now().toString().slice(-6),
              status: 'SIMULATED_SUCCESS',
              amount: appState.amount,
              paymentMethod: paymentMethods[appState.paymentMethod].name,
              validationMessage: 'Offline simulation - payment would be processed',
              patternMatchingSteps: mockSteps
            }
          }
        });

        // **FIX 1:** Update status panel AFTER offline simulation
        updateStatusIndicators();
        updateProcessingResults({
            status: 'SIMULATED_SUCCESS',
            transactionId: 'TXN-OFFLINE-' + Date.now().toString().slice(-6),
            validationMessage: 'Offline simulation - payment would be processed'
        });

      }, 1000);
    }
  } catch (error) {
    console.error('Payment processing failed:', error);
  } finally {
    setTimeout(() => {
      appState.processing = false;
      processBtn.disabled = false;
      processBtn.classList.remove('processing');
      processBtn.innerHTML = originalText;
    }, 1000); // Shortened delay
  }
}

function updateProcessingResults(paymentResponse) {
  // This function now just adds the final transaction details
  document.getElementById('processing-status').textContent =
    `${paymentResponse.status} - Transaction: ${paymentResponse.transactionId}`;

  if (paymentResponse.validationMessage) {
    const validationIcon = document.querySelector('#status-section .status-item:nth-child(3) .status-icon');
    if(paymentResponse.status === 'SUCCESS' || paymentResponse.status === 'PENDING' || paymentResponse.status === 'SIMULATED_SUCCESS') {
        validationIcon.className = 'status-icon success';
        validationIcon.textContent = '✓';
    } else {
        validationIcon.className = 'status-icon warning';
        validationIcon.textContent = '⚠';
    }
    document.getElementById('validation-status').textContent = paymentResponse.validationMessage;
  }
}

// **NOTE:** This function is no longer called by inputs,
// but is left here in case you want to re-enable "live preview" later.
function updatePatternHighlighting() {
  document.querySelectorAll('.pattern-line').forEach(line => line.classList.remove('highlighted'));

  const linesToHighlight = [];
  const switchLine = document.querySelector('[data-pattern="switch"]'); linesToHighlight.push(switchLine);

  const methodLine = document.querySelector(`[data-pattern="${appState.paymentMethod}"]`);
  if (methodLine) linesToHighlight.push(methodLine);

  if (appState.amount > 1000 || appState.isInternational) {
    const guardLine = document.querySelector('[data-pattern="guard"]');
    linesToHighlight.push(guardLine);
  }

  const sealedLine = document.querySelector('[data-pattern="sealed"]'); linesToHighlight.push(sealedLine);

  linesToHighlight.forEach(line => { if (line) line.classList.add('highlighted'); });
}

function updateStatusIndicators() {
  // This function now correctly acts as the "result" display,
  // called ONLY after processing.
  const method = paymentMethods[appState.paymentMethod];
  const customer = customerTypes[appState.customerType];

  document.getElementById('payment-detection').textContent = `Pattern: ${method.name} identified`;

  let guardText; let guardIcon;
  if (appState.amount > 1000) {
    guardText = `Amount > $1,000 — High-value guard evaluated`;
    guardIcon = document.querySelector('#status-section .status-item:nth-child(2) .status-icon');
    guardIcon.className = 'status-icon warning'; guardIcon.textContent = '⚠';
  } else {
    guardText = `Amount ≤ $1,000 — Standard processing`;
    guardIcon = document.querySelector('#status-section .status-item:nth-child(2) .status-icon');
    guardIcon.className = 'status-icon success'; guardIcon.textContent = '✓';
  }
  document.getElementById('guard-condition').textContent = guardText;

  let validationText = `${customer.tier} customer - ${method.validation}`;
  if (appState.isInternational) validationText += ' + International compliance';
  document.getElementById('validation-status').textContent = validationText;

  // This line will be overwritten by updateProcessingResults, which is correct.
  document.getElementById('processing-status').textContent = `Processing...`;
}

async function initApp() {
  // **FIX 1:** Removed calls to updatePatternHighlighting() and updateStatusIndicators()

  // Reset panels to default on load
  resetStatusPanels();

  updateAmountDisplays(appState.amount);
  updateOrderDisplay(scenarios[500]);

  document.querySelector('[data-amount="500"]').classList.add('active');

  const logId = createFlowLog('App Initialization', 'GET', '/api/payment/demo-state');
  const connected = await checkConnection();

  if (connected) {
    updateFlowLog(logId, {
      controller_method: 'PaymentController.getDemoState',
      operation_description: 'Backend connected - Demo synchronized with server state'
    });
  } else {
    updateFlowLog(logId, {
      controller_method: 'Frontend Only Mode',
      operation_description: 'Backend offline - Running in simulation mode'
    });
  }
}

document.addEventListener('DOMContentLoaded', initApp);
setInterval(checkConnection, 60000);