/**
 * TechMart Shopping Cart Demo - Backend Integrated Implementation
 * Demonstrates Java 21 Sequenced Collections through real backend API calls
 */

/* ================================
   DEMO CONFIGURATION
   ================================ */

const DEMO_CONFIG = {
    customerId: 1,
    baseUrl: 'http://localhost:8080'  // Backend Spring Boot URL
};

/* ================================
   VISUAL FLOW INSPECTOR FUNCTIONS
   ================================ */

function createFlowLog(userAction, method, endpoint) {
    const logContainer = document.getElementById('api-log');
    if (!logContainer) {
        console.error('VFI: Cannot create flow log - container not found');
        return null;
    }

    // Clear initial message if present
    const initialMessages = logContainer.querySelectorAll('.text-muted, .text-center');
    initialMessages.forEach(msg => {
        if (msg.textContent && msg.textContent.includes('Click')) {
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
        <div class="api-flow-child" data-role="controller">🔴 Controller: Pending...</div>
    `;

    logContainer.insertBefore(flowBlock, logContainer.firstChild);

    // Maintain entry limit
    while (logContainer.children.length > 12) {
        logContainer.removeChild(logContainer.lastChild);
    }

    logContainer.scrollTop = 0;
    return logId;
}

function updateFlowLog(logId, responseData) {
    console.log('🔍 updateFlowLog called with logId:', logId);
    console.log('🔍 responseData:', responseData);

    if (!logId) {
        console.log('❌ No logId provided');
        return;
    }

    const flowBlock = document.getElementById(logId);
    if (!flowBlock) {
        console.log('❌ Flow block not found for logId:', logId);
        return;
    }

    const controllerElement = flowBlock.querySelector('[data-role="controller"]');
    if (!controllerElement) {
        console.log('❌ Controller element not found');
        return;
    }

    // Handle error responses
    if (responseData.error) {
        controllerElement.innerHTML = `🔴 Controller: <span style="color: #ef4444;">${responseData.error}</span>`;
        return;
    }

    let html = `🔴 Controller: <strong>${responseData.controllerMethod || responseData.controller_method || 'ShoppingCartController.handleRequest'}</strong>`;

    // Clear previous highlights before showing new ones
    console.log('🧹 Clearing all previous highlights');
    clearAllMethodHighlights();

    // Collect all methods to highlight
    const methodsToHighlight = [];

    // Handle service calls
    if (responseData.serviceCalls && typeof responseData.serviceCalls === 'object') {
        console.log('📦 serviceCalls found:', responseData.serviceCalls);
        html += `<div class="api-flow-child">🟣 Service Layer: Multiple methods called</div>`;

        Object.entries(responseData.serviceCalls).forEach(([serviceMethod, java21Methods]) => {
            console.log(`  📌 Processing service: ${serviceMethod}`, java21Methods);
            if (java21Methods && java21Methods.length > 0) {
                html += `<div class="api-flow-child">  └── <strong>${serviceMethod}</strong> → <span class="java21-method-tag">${java21Methods.join(', ')}</span></div>`;
                // Collect methods to highlight
                java21Methods.forEach(method => {
                    console.log('    ➕ Adding method to highlight:', method);
                    methodsToHighlight.push(method);
                });
            } else {
                html += `<div class="api-flow-child">  └── <strong>${serviceMethod}</strong> → Standard Collection API</div>`;
            }
        });
    } else {
        console.log('⚠️ No serviceCalls in responseData');
    }

    if (responseData.operationDescription) {
        html += `<div class="api-flow-child">💡 Operation: ${responseData.operationDescription}</div>`;
    }

    controllerElement.innerHTML = html;

    // Highlight all collected methods after DOM update
    console.log('✨ Total methods to highlight:', methodsToHighlight);
    setTimeout(() => {
        console.log('⏰ Now highlighting methods...');
        methodsToHighlight.forEach(method => {
            console.log('  🎯 Calling highlightJavaMethod for:', method);
            highlightJavaMethod(method);
        });
    }, 50);
}

function clearInspectorLog() {
    const logContainer = document.getElementById('api-log');
    if (!logContainer) return;

    logContainer.innerHTML = '<div class="text-muted text-center py-2">Log cleared. Click an action to see the call stack...</div>';
}

function highlightJavaMethod(methodName) {
    if (!methodName) return;

    const safe = String(methodName).replace(/\(\)$/, '');
    console.log('Highlighting method:', safe);

    // Find target row
    const row = document.getElementById(`code-${safe}`);
    if (!row) {
        console.log('Row not found for:', safe);
        return;
    }

    console.log('Found row for:', safe);

    // Apply highlighting
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

    // Auto-remove highlighting
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
    }, 2500);
}

function clearAllMethodHighlights() {
    // Clear all previous highlights
    document.querySelectorAll('tr[data-highlight="1"]').forEach(row => {
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
}

/* ================================
   API ACTION FUNCTIONS - SHOPPING CART OPERATIONS
   ================================ */

function addProductToCart(productId, productName, price) {
    const payload = { productId, productName, quantity: 1, price };
    apiCall('POST', `/api/cart/${DEMO_CONFIG.customerId}/items`, payload, `Add ${productName}`);
}

function addPriorityItem(productId, productName, price) {
    const payload = { productId, productName, quantity: 1, price };
    apiCall('POST', `/api/cart/${DEMO_CONFIG.customerId}/priority-items`, payload, `Add ${productName} (Priority)`);
}

function undoLastAction() {
    apiCall('POST', `/api/cart/${DEMO_CONFIG.customerId}/undo`, null, 'Undo Last Action');
}

function clearCart() {
    apiCall('DELETE', `/api/cart/${DEMO_CONFIG.customerId}`, null, 'Clear Cart');
}

function removeCartItem(itemId, itemName) {
    apiCall('DELETE', `/api/cart/${DEMO_CONFIG.customerId}/items/${itemId}`, null, `Remove ${itemName}`);
}

/* ================================
   CORE API COMMUNICATION - REAL HTTP REQUESTS
   ================================ */

async function apiCall(method, endpoint, body = null, userAction) {
    console.log(`📄 API Call: ${method} ${endpoint} for ${userAction}`);

    const logId = createFlowLog(userAction, method, endpoint);

    try {
        const response = await fetch(`${DEMO_CONFIG.baseUrl}${endpoint}`, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: body ? JSON.stringify(body) : null
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();
        console.log(`✅ API Response for ${userAction}:`, result);

        updateFlowLog(logId, result);

        // Fetch updated cart state after operations
        if (endpoint.includes('/cart/')) {
            const cartResponse = await fetch(`${DEMO_CONFIG.baseUrl}/api/cart/${DEMO_CONFIG.customerId}`);
            if (cartResponse.ok) {
                const cartState = await cartResponse.json();
                updateCartUI(cartState);
            }
        }

        return result;

    } catch (error) {
        console.error(`❌ API Error for ${userAction}:`, error);

        updateFlowLog(logId, {
            error: error.message || 'Request Failed',
            controllerMethod: 'Error - Backend Unavailable'
        });

        showNotification(error.message || 'Request Failed', 'danger');
        throw error;
    }
}

/* ================================
   CART UI MANAGEMENT
   ================================ */

function updateCartUI(cartData) {
    const container = document.getElementById('cart-items-display');
    const undoBtn = document.getElementById('undo-btn');

    if (!container) {
        console.error('Cart items display container not found');
        return;
    }

    container.innerHTML = '';

    // Handle empty cart
    if (!cartData.items || cartData.items.length === 0) {
        container.innerHTML = '<div class="text-muted text-center py-3">Cart is empty</div>';
        if (undoBtn) undoBtn.disabled = true;
        return;
    }

    // Enable undo button when cart has items
    if (undoBtn) undoBtn.disabled = cartData.actionHistory.length === 0;

    // Render each cart item
    cartData.items.forEach((item, index) => {
        const isFirst = index === 0 && cartData.items.length > 1;
        const isLast = index === cartData.items.length - 1 && cartData.items.length > 1;

        // Create badges to show Java 21 Sequenced Collections insights
        let badges = '';

        // Use actual metadata from backend
        if (cartData.oldestItem && item.product.name === cartData.oldestItem.name) {
            badges += '<span class="badge bg-success ms-2" title="Retrieved via getFirst()">First Added (getFirst)</span>';
        }
        if (cartData.newestItem && item.product.name === cartData.newestItem.name) {
            badges += '<span class="badge bg-primary ms-2" title="Retrieved via getLast()">Last Added (getLast)</span>';
        }

        // Fallback badges based on position if metadata not available
        if (!badges) {
            if (isFirst) badges += '<span class="badge bg-success ms-2">First Added</span>';
            if (isLast) badges += '<span class="badge bg-primary ms-2">Last Added</span>';
        }

        const itemHtml = `
            <div class="cart-item-row">
                <div class="item-details">
                    <strong>${index + 1}. ${item.product.name}</strong> - $${item.unitPrice.toLocaleString()}
                    ${badges}
                </div>
                <button class="btn btn-sm btn-outline-danger remove-btn"
                        onclick="removeCartItem(${item.id}, '${item.product.name.replace(/'/g, "\\'")}')">
                    Remove
                </button>
            </div>`;

        container.innerHTML += itemHtml;
    });

    // Add Java 21 Sequenced Collections metadata section
    if (cartData.oldestItem || cartData.newestItem) {
        let metadataHtml = `
            <div class="cart-metadata mt-3 p-2" style="background: #f8f9fa; border-radius: 6px; font-size: 0.85rem;">
                <strong>🔍 Java 21 Sequenced Collections Metadata:</strong><br>`;

        if (cartData.oldestItem) {
            metadataHtml += `📅 <strong>Oldest Item</strong> (via getFirst()): ${cartData.oldestItem.name}<br>`;
        }
        if (cartData.newestItem) {
            metadataHtml += `🆕 <strong>Newest Item</strong> (via getLast()): ${cartData.newestItem.name}<br>`;
        }

        metadataHtml += '</div>';
        container.innerHTML += metadataHtml;
    }

    console.log(`📋 Cart UI updated: ${cartData.items.length} items displayed`);
}

/* ================================
   NOTIFICATION SYSTEM
   ================================ */

function showNotification(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) {
        console.log(`${type.toUpperCase()}: ${message}`);
        return;
    }

    const notification = document.createElement('div');
    notification.className = `alert alert-${type} alert-dismissible fade show`;
    notification.innerHTML = `
        <span>${message}</span>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;

    container.appendChild(notification);

    // Auto-remove after 4 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, 4000);
}

/* ================================
   DEMO INITIALIZATION
   ================================ */

async function loadInitialCartState() {
    try {
        const response = await fetch(`${DEMO_CONFIG.baseUrl}/api/cart/${DEMO_CONFIG.customerId}`);
        if (response.ok) {
            const cartState = await response.json();
            updateCartUI(cartState);
        } else {
            // Backend not available or cart doesn't exist yet
            updateCartUI({ items: [], actionHistory: [], oldestItem: null, newestItem: null });
        }
    } catch (error) {
        console.log('Backend not available, showing empty cart');
        updateCartUI({ items: [], actionHistory: [], oldestItem: null, newestItem: null });
    }
}

function initializeShoppingCartDemo() {
    console.log('🚀 Shopping Cart Demo Initializing...');

    // Load initial cart state from backend
    loadInitialCartState();

    // Show welcome message in Visual Flow Inspector
    const logId = createFlowLog('Demo Initialization', 'GET', '/api/demo/init');
    setTimeout(async () => {
        try {
            const response = await fetch(`${DEMO_CONFIG.baseUrl}/api/demo/init`);
            if (response.ok) {
                const result = await response.json();
                updateFlowLog(logId, result);
            } else {
                updateFlowLog(logId, {
                    controllerMethod: 'DemoController.initialize',
                    serviceCalls: {
                        'DemoService.setupSequencedCollections': ['LinkedHashSet', 'ArrayList']
                    },
                    operationDescription: 'Shopping Cart Demo ready with Java 21 Sequenced Collections'
                });
            }
        } catch (error) {
            updateFlowLog(logId, {
                controllerMethod: 'DemoController.initialize',
                serviceCalls: {
                    'DemoService.setupSequencedCollections': ['LinkedHashSet', 'ArrayList']
                },
                operationDescription: 'Shopping Cart Demo ready with Java 21 Sequenced Collections (Frontend-only mode)'
            });
        }
    }, 500);

    console.log('✅ Shopping Cart Demo Ready');
    console.log('🔧 Backend URL:', DEMO_CONFIG.baseUrl);
    console.log('👤 Customer ID:', DEMO_CONFIG.customerId);
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializeShoppingCartDemo();
});

/* ================================
   DEVELOPER DEBUGGING UTILITIES
   ================================ */

window.CartDemo = {
    // API actions
    addProduct: addProductToCart,
    addPriority: addPriorityItem,
    undo: undoLastAction,
    clear: clearCart,

    // Direct API access
    directAPI: apiCall,

    // Visual Flow Inspector access
    clearLog: clearInspectorLog,

    // State inspection
    getConfig: () => DEMO_CONFIG,

    // Test functions
    testSequence: async () => {
        console.log('🧪 Running test sequence...');
        try {
            await addProductToCart(1, 'Test iPhone', 999);
            await new Promise(resolve => setTimeout(resolve, 1000));
            await addProductToCart(2, 'Test AirPods', 249);
            await new Promise(resolve => setTimeout(resolve, 1000));
            await addPriorityItem(3, 'Test MacBook', 1999);
            console.log('✅ Test sequence completed');
        } catch (error) {
            console.error('❌ Test sequence failed:', error);
        }
    }
};

console.log('🚀 Shopping Cart Demo loaded successfully');
console.log('🎮 Try: CartDemo.testSequence() or CartDemo.addProduct(1, "iPhone", 999)');