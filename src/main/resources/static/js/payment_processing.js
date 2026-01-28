/*
* Simplified payment_processing.js
* - Removed connection state tracking
* - Removed offline/online mode detection
* - Just shows API call results: success or error
*/

const API_BASE_URL = 'http://localhost:8080';
const ENDPOINTS = {
  PROCESS: '/api/payment/process',
  DEMO_STATE: '/api/payment/demo-state'
};

let appState = {
  amount: 500,
  paymentMethod: 'credit',
  customerType: 'basic',
  isInternational: false,
  processing: false
};

const orderScenarios = {
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

async function apiCall(method, endpoint, data = null) {
  const requestMethod = method.toUpperCase();
  const logId = createFlowLog(`${requestMethod} Request`, requestMethod, endpoint);

  try {
    const config = {
      method: requestMethod,
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include'
    };
    if (data && (requestMethod === 'POST' || requestMethod === 'PUT')) {
      config.body = JSON.stringify(data);
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, config);
    if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);

    const responseData = await response.json();

    updateFlowLog(logId, {
      controller_method: responseData.controller_method || 'PaymentController',
      operation_description: responseData.operation_description || 'Operation completed',
      response_data: responseData
    });

    return responseData;

  } catch (error) {
    console.error('API Call failed:', error);
    updateFlowLog(logId, { error: error.message });
    throw error;
  }
}

function createFlowLog(userAction, method, endpoint) {
  const logContainer = document.getElementById('api-log');
  if (!logContainer) return null;

  const flowBlock = document.createElement('div');
  const logId = `flow-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  flowBlock.id = logId;
  flowBlock.className = 'api-flow-block';

  flowBlock.innerHTML = `
    <div>👤 <strong>${userAction}</strong> (Frontend)</div>
    <div class="api-flow-child" data-role="status">🌐 ${method} ${endpoint}</div>
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

  // Clear highlights before rendering new ones
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
      highlightMatchedPattern('switch');
      if (stepMessage.includes('CreditCard')) highlightMatchedPattern('credit');
      else if (stepMessage.includes('PayPal')) highlightMatchedPattern('paypal');
      else if (stepMessage.includes('BankTransfer')) highlightMatchedPattern('bank');
    } else if (stepType === 'DESTRUCTURING') {
      html += `<div class="pm-step-detail success">✓ ${stepMessage}</div>`;
      if (stepMessage.includes('CreditCard')) highlightMatchedPattern('credit');
      else if (stepMessage.includes('PayPal')) highlightMatchedPattern('paypal');
      else if (stepMessage.includes('BankTransfer')) highlightMatchedPattern('bank');
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
      if (isPassed) highlightMatchedPattern('guard');
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

  // Handle errors
  if (responseData.error) {
    flowBlock.classList.add('error');
    controllerElement.innerHTML = `🔴 <span class="error-text">Error: ${responseData.error}</span>`;
    return;
  }

  const actualData = responseData.response_data || responseData;
  const controllerMethod = actualData.controllerMethod || responseData.controller_method || 'PaymentController';
  const operationDesc = actualData.operationDescription || responseData.operation_description || 'Operation completed';

  flowBlock.classList.add('success');
  let html = `🔴 Controller: <strong>${controllerMethod}</strong>`;

  // Find pattern matching steps
  let pmSteps = null;
  if (actualData?.metadata?.paymentResponse?.patternMatchingSteps) {
    pmSteps = actualData.metadata.paymentResponse.patternMatchingSteps;
  } else if (actualData?.paymentResponse?.patternMatchingSteps) {
    pmSteps = actualData.paymentResponse.patternMatchingSteps;
  } else if (actualData?.patternMatchingSteps) {
    pmSteps = actualData.patternMatchingSteps;
  }

  if (pmSteps && pmSteps.length > 0) {
    html += renderPatternMatchingSteps(pmSteps);
  }

  if (operationDesc) {
    html += `<div class="api-flow-child">💡 <span class="success-text">${operationDesc}</span></div>`;
  }

  // Transaction details
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
  logContainer.innerHTML = '';
  resetStatusPanels();
}

function resetStatusPanels() {
  document.querySelectorAll('.pattern-line.highlighted').forEach(line => line.classList.remove('highlighted'));

  document.getElementById('payment-detection').textContent = 'Pattern: Awaiting processing...';

  const guardIcon = document.querySelector('#status-section .status-item:nth-child(2) .status-icon');
  guardIcon.className = 'status-icon success';
  guardIcon.textContent = '✓';
  document.getElementById('guard-condition').textContent = 'Guard: Awaiting processing...';

  const validationIcon = document.querySelector('#status-section .status-item:nth-child(3) .status-icon');
  validationIcon.className = 'status-icon success';
  validationIcon.textContent = '✓';
  document.getElementById('validation-status').textContent = 'Ready for processing';

  const processingIcon = document.querySelector('#status-section .status-item:nth-child(4) .status-icon');
  processingIcon.className = 'status-icon play';
  processingIcon.textContent = '▶';
  document.getElementById('processing-status').textContent = 'Click Process Payment to execute';
}

function highlightMatchedPattern(methodName) {
  if (!methodName) return;
  const safe = String(methodName).replace(/\(\)$/, '');

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
    if (line) line.classList.add('highlighted');
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

function setAmount(amount) {
  document.querySelectorAll('.amount-btn').forEach(btn => btn.classList.remove('active'));
  if (typeof event !== 'undefined' && event.target) event.target.classList.add('active');

  appState.amount = amount;
  updateOrderDisplay(orderScenarios[amount]);
  updateAmountDisplays(amount);
}

function selectPaymentMethod(method) {
  document.querySelectorAll('.payment-method').forEach(pm => pm.classList.remove('selected'));
  if (typeof event !== 'undefined' && event.target) {
    const card = event.target.closest('.payment-method');
    if (card) card.classList.add('selected');
  }
  appState.paymentMethod = method;
}

function selectCustomerType(type) {
  document.querySelectorAll('.customer-tab').forEach(tab => tab.classList.remove('active'));
  if (typeof event !== 'undefined' && event.target) event.target.classList.add('active');
  appState.customerType = type;
}

function toggleInternational() {
  appState.isInternational = document.getElementById('international').checked;
}

async function processPayment() {
  if (appState.processing) return;

  appState.processing = true;
  const processBtn = document.getElementById('processBtn');
  const originalText = processBtn.innerHTML;
  processBtn.disabled = true;
  processBtn.classList.add('processing');
  processBtn.innerHTML = '<div class="spinner"></div> Processing...';

  resetStatusPanels();

  try {
    const paymentRequest = {
      customerId: 1,
      paymentMethod: appState.paymentMethod,
      amount: appState.amount,
      customerType: appState.customerType.toUpperCase(),
      international: appState.isInternational
    };

    console.log('Sending payment request:', paymentRequest);

    const response = await apiCall('POST', ENDPOINTS.PROCESS, paymentRequest);

    console.log('Payment response received:', response);

    updateStatusIndicators();

    // Extract payment response
    let paymentResponseData = null;
    if (response?.response_data?.metadata?.paymentResponse) {
      paymentResponseData = response.response_data.metadata.paymentResponse;
    } else if (response?.response_data?.paymentResponse) {
      paymentResponseData = response.response_data.paymentResponse;
    } else if (response?.metadata?.paymentResponse) {
      paymentResponseData = response.metadata.paymentResponse;
    } else if (response?.paymentResponse) {
      paymentResponseData = response.paymentResponse;
    }

    if (paymentResponseData) {
      console.log('Payment response data:', paymentResponseData);
      updateProcessingResults(paymentResponseData);
    }

  } catch (error) {
    console.error('Payment processing failed:', error);
  } finally {
    setTimeout(() => {
      appState.processing = false;
      processBtn.disabled = false;
      processBtn.classList.remove('processing');
      processBtn.innerHTML = originalText;
    }, 1000);
  }
}

function updateProcessingResults(paymentResponse) {
  document.getElementById('processing-status').textContent =
    `${paymentResponse.status} - Transaction: ${paymentResponse.transactionId}`;

  if (paymentResponse.validationMessage) {
    const validationIcon = document.querySelector('#status-section .status-item:nth-child(3) .status-icon');
    if (paymentResponse.status === 'SUCCESS' || paymentResponse.status === 'PENDING' || paymentResponse.status === 'SIMULATED_SUCCESS') {
      validationIcon.className = 'status-icon success';
      validationIcon.textContent = '✓';
    } else {
      validationIcon.className = 'status-icon warning';
      validationIcon.textContent = '⚠';
    }
    document.getElementById('validation-status').textContent = paymentResponse.validationMessage;
  }
}

function updateStatusIndicators() {
  const method = paymentMethods[appState.paymentMethod];
  const customer = customerTypes[appState.customerType];

  document.getElementById('payment-detection').textContent = `Pattern: ${method.name} identified`;

  let guardText;
  let guardIcon = document.querySelector('#status-section .status-item:nth-child(2) .status-icon');
  if (appState.amount > 1000) {
    guardText = `Amount > $1,000 — High-value guard evaluated`;
    guardIcon.className = 'status-icon warning';
    guardIcon.textContent = '⚠';
  } else {
    guardText = `Amount ≤ $1,000 — Standard processing`;
    guardIcon.className = 'status-icon success';
    guardIcon.textContent = '✓';
  }
  document.getElementById('guard-condition').textContent = guardText;

  let validationText = `${customer.tier} customer - ${method.validation}`;
  if (appState.isInternational) validationText += ' + International compliance';
  document.getElementById('validation-status').textContent = validationText;

  document.getElementById('processing-status').textContent = `Processing...`;
}

function initApp() {
  resetStatusPanels();
  updateAmountDisplays(appState.amount);
  updateOrderDisplay(orderScenarios[500]);
  document.querySelector('[data-amount="500"]').classList.add('active');
}

document.addEventListener('DOMContentLoaded', initApp);