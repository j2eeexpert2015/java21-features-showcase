// RSA vs KEM Comparison - JavaScript
const API_BASE = 'http://localhost:8080/api/crypto';
const state = {
    rsa: { publicKey: null, privateKey: null, aesKey: null, encryptedKey: null, decryptedKey: null, iv: null, ciphertext: null },
    kem: { publicKey: null, privateKey: null, encapsulation: null, sharedSecret: null, derivedSecret: null, iv: null, ciphertext: null }
};

function updateProgress(method, percentage) {
    document.getElementById(`${method}-progress`).style.width = `${percentage}%`;
    document.getElementById(`${method}-progress-text`).textContent = `${percentage}%`;
}

function completeStep(method, stepNum) {
    const step = document.getElementById(`${method}-step${stepNum}`);
    step.classList.add('completed'); step.classList.remove('active');
    document.getElementById(`${method}-step${stepNum}-status`).textContent = '✅';
}

function activateStep(method, stepNum) {
    const step = document.getElementById(`${method}-step${stepNum}`);
    step.classList.add('active');
    document.getElementById(`${method}-step${stepNum}-status`).textContent = '▶️';
}

function showError(method, stepNum, errorMessage) {
    const step = document.getElementById(`${method}-step${stepNum}`);
    step.classList.add('active');
    const errorDiv = document.createElement('div');
    errorDiv.className = 'info-box danger'; errorDiv.style.marginTop = '6px';
    errorDiv.innerHTML = `❌ Error: ${errorMessage}`;
    const content = document.getElementById(`${method}-step${stepNum}-content`);
    content.style.display = 'block'; content.appendChild(errorDiv);
    document.getElementById(`${method}-step${stepNum}-status`).textContent = '❌';
}

// RSA Step 1: Generate Key Pair
document.getElementById('rsa-btn-step1').addEventListener('click', async function() {
    activateStep('rsa', 1);
    try {
        const response = await fetch(`${API_BASE}/rsa/generate-keypair`);
        const data = await response.json();
        if (data.success) {
            state.rsa.publicKey = data.data.publicKey; state.rsa.privateKey = data.data.privateKey;
            document.getElementById('rsa-step1-content').style.display = 'block';
            document.getElementById('debug-rsa-public').textContent = state.rsa.publicKey.substring(0, 64) + '...';
            document.getElementById('debug-rsa-private').textContent = state.rsa.privateKey.substring(0, 64) + '...';
            completeStep('rsa', 1); updateProgress('rsa', 25);
            document.getElementById('rsa-btn-step2').disabled = false;
        } else { showError('rsa', 1, data.error || 'Failed to generate RSA key pair'); }
    } catch (error) { showError('rsa', 1, error.message); console.error('RSA Key Generation Error:', error); }
});

// RSA Step 2: Generate AES Key
document.getElementById('rsa-btn-step2').addEventListener('click', async function() {
    activateStep('rsa', 2);
    try {
        const response = await fetch(`${API_BASE}/rsa/generate-aes-key`);
        const data = await response.json();
        if (data.success) {
            state.rsa.aesKey = data.data.aesKey;
            document.getElementById('rsa-step2-content').style.display = 'block';
            document.getElementById('rsa-aes-key').textContent = state.rsa.aesKey;
            document.getElementById('debug-rsa-aes').textContent = state.rsa.aesKey;
            completeStep('rsa', 2); updateProgress('rsa', 50);
            document.getElementById('rsa-btn-step3').disabled = false;
        } else { showError('rsa', 2, data.error || 'Failed to generate AES key'); }
    } catch (error) { showError('rsa', 2, error.message); console.error('AES Key Generation Error:', error); }
});

// RSA Step 3: Encrypt AES Key
document.getElementById('rsa-btn-step3').addEventListener('click', async function() {
    activateStep('rsa', 3);
    try {
        const requestData = {
            publicKey: state.rsa.publicKey,
            aesKey: state.rsa.aesKey
        };
        const response = await fetch(`${API_BASE}/rsa/encrypt-aes-key`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(requestData)
        });
        const data = await response.json();
        if (data.success) {
            state.rsa.encryptedKey = data.data.encryptedKey;
            document.getElementById('rsa-step3-content').style.display = 'block';
            document.getElementById('rsa-encrypted-key').textContent = state.rsa.encryptedKey;
            document.getElementById('debug-rsa-encrypted').textContent = state.rsa.encryptedKey;
            completeStep('rsa', 3); updateProgress('rsa', 75);
            document.getElementById('rsa-btn-step4').disabled = false;
        } else { showError('rsa', 3, data.error || 'Failed to encrypt AES key'); }
    } catch (error) { showError('rsa', 3, error.message); console.error('RSA Encryption Error:', error); }
});

// RSA Step 4: Decrypt AES Key
document.getElementById('rsa-btn-step4').addEventListener('click', async function() {
    activateStep('rsa', 4);
    try {
        const requestData = {
            privateKey: state.rsa.privateKey,
            encryptedKey: state.rsa.encryptedKey,
            originalKey: state.rsa.aesKey
        };
        const response = await fetch(`${API_BASE}/rsa/decrypt-aes-key`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(requestData)
        });
        const data = await response.json();
        if (data.success) {
            state.rsa.decryptedKey = data.data.decryptedKey;
            document.getElementById('rsa-step4-content').style.display = 'block';
            document.getElementById('rsa-decrypted-key').textContent = state.rsa.decryptedKey;
            document.getElementById('debug-rsa-recovered').textContent = state.rsa.decryptedKey;
            document.getElementById('rsa-match').textContent = data.data.keysMatch;
            completeStep('rsa', 4); updateProgress('rsa', 100); checkBothReady();
        } else { showError('rsa', 4, data.error || 'Failed to decrypt AES key'); }
    } catch (error) { showError('rsa', 4, error.message); console.error('RSA Decryption Error:', error); }
});

// KEM Step 1: Generate Key Pair
document.getElementById('kem-btn-step1').addEventListener('click', async function() {
    activateStep('kem', 1);
    try {
        const response = await fetch(`${API_BASE}/kem/generate-keypair`);
        const data = await response.json();
        if (data.success) {
            state.kem.publicKey = data.data.publicKey; state.kem.privateKey = data.data.privateKey;
            document.getElementById('kem-step1-content').style.display = 'block';
            document.getElementById('debug-kem-public').textContent = state.kem.publicKey;
            document.getElementById('debug-kem-private').textContent = state.kem.privateKey;
            completeStep('kem', 1); updateProgress('kem', 25);
            document.getElementById('kem-btn-step2').disabled = false;
        } else { showError('kem', 1, data.error || 'Failed to generate X25519 key pair'); }
    } catch (error) { showError('kem', 1, error.message); console.error('KEM Key Generation Error:', error); }
});

// KEM Step 2: Encapsulation
document.getElementById('kem-btn-step2').addEventListener('click', async function() {
    activateStep('kem', 2);
    try {
        const requestData = {
            publicKey: state.kem.publicKey
        };
        const response = await fetch(`${API_BASE}/kem/encapsulate`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(requestData)
        });
        const data = await response.json();
        if (data.success) {
            state.kem.sharedSecret = data.data.sharedSecret; state.kem.encapsulation = data.data.encapsulation;
            document.getElementById('kem-step2-content').style.display = 'block';
            document.getElementById('kem-shared-secret').textContent = state.kem.sharedSecret;
            document.getElementById('kem-encapsulation').textContent = state.kem.encapsulation;
            document.getElementById('debug-kem-secret').textContent = state.kem.sharedSecret;
            document.getElementById('debug-kem-encapsulation').textContent = state.kem.encapsulation;
            completeStep('kem', 2); updateProgress('kem', 50);
            document.getElementById('kem-btn-step3').disabled = false;
        } else { showError('kem', 2, data.error || 'Failed to encapsulate'); }
    } catch (error) { showError('kem', 2, error.message); console.error('KEM Encapsulation Error:', error); }
});

// KEM Step 3: Transmit Encapsulation
document.getElementById('kem-btn-step3').addEventListener('click', function() {
    activateStep('kem', 3);
    setTimeout(() => {
        document.getElementById('kem-step3-content').style.display = 'block';
        completeStep('kem', 3); updateProgress('kem', 75);
        document.getElementById('kem-btn-step4').disabled = false;
    }, 300);
});

// KEM Step 4: Decapsulation
document.getElementById('kem-btn-step4').addEventListener('click', async function() {
    activateStep('kem', 4);
    try {
        const requestData = {
            privateKey: state.kem.privateKey,
            encapsulation: state.kem.encapsulation,
            originalSecret: state.kem.sharedSecret
        };
        const response = await fetch(`${API_BASE}/kem/decapsulate`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(requestData)
        });
        const data = await response.json();
        if (data.success) {
            state.kem.derivedSecret = data.data.derivedSecret;
            document.getElementById('kem-step4-content').style.display = 'block';
            document.getElementById('kem-derived-secret').textContent = state.kem.derivedSecret;
            document.getElementById('debug-kem-derived').textContent = state.kem.derivedSecret;
            document.getElementById('kem-match').textContent = data.data.secretsMatch;
            completeStep('kem', 4); updateProgress('kem', 100); checkBothReady();
        } else { showError('kem', 4, data.error || 'Failed to decapsulate'); }
    } catch (error) { showError('kem', 4, error.message); console.error('KEM Decapsulation Error:', error); }
});

function checkBothReady() {
    if (state.rsa.decryptedKey && state.kem.derivedSecret) {
        document.getElementById('btn-encrypt-both').disabled = false;
    }
}

// Encrypt Message with Both Methods
document.getElementById('btn-encrypt-both').addEventListener('click', async function() {
    const message = document.getElementById('message-input').value;
    if (!message) { alert('Please enter a message to encrypt'); return; }
    try {
        const rsaEncryptResponse = await fetch(`${API_BASE}/encrypt-message`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: message,
                aesKey: state.rsa.decryptedKey
            })
        });
        const rsaEncryptData = await rsaEncryptResponse.json();
        if (rsaEncryptData.success) {
            state.rsa.ciphertext = rsaEncryptData.data.ciphertext; state.rsa.iv = rsaEncryptData.data.iv;
            const rsaDecryptResponse = await fetch(`${API_BASE}/decrypt-message`, {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    ciphertext: state.rsa.ciphertext,
                    iv: state.rsa.iv,
                    aesKey: state.rsa.decryptedKey
                })
            });
            const rsaDecryptData = await rsaDecryptResponse.json();
            if (rsaDecryptData.success) {
                document.getElementById('debug-rsa-iv').textContent = state.rsa.iv;
                document.getElementById('rsa-ciphertext').textContent = state.rsa.ciphertext;
                document.getElementById('rsa-plaintext').textContent = rsaDecryptData.data.plaintext;
            }
        }
        const kemEncryptResponse = await fetch(`${API_BASE}/encrypt-message`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: message,
                aesKey: state.kem.derivedSecret
            })
        });
        const kemEncryptData = await kemEncryptResponse.json();
        if (kemEncryptData.success) {
            state.kem.ciphertext = kemEncryptData.data.ciphertext; state.kem.iv = kemEncryptData.data.iv;
            const kemDecryptResponse = await fetch(`${API_BASE}/decrypt-message`, {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    ciphertext: state.kem.ciphertext,
                    iv: state.kem.iv,
                    aesKey: state.kem.derivedSecret
                })
            });
            const kemDecryptData = await kemDecryptResponse.json();
            if (kemDecryptData.success) {
                document.getElementById('debug-kem-iv').textContent = state.kem.iv;
                document.getElementById('kem-ciphertext').textContent = state.kem.ciphertext;
                document.getElementById('kem-plaintext').textContent = kemDecryptData.data.plaintext;
            }
        }
        document.getElementById('encryption-results').style.display = 'block';
    } catch (error) {
        alert('Encryption failed: ' + error.message);
        console.error('Encryption Error:', error);
    }
});

// Reset Button
document.getElementById('btn-reset').addEventListener('click', function() {
    if (confirm('Reset both sessions? This will clear all generated keys and data.')) {
        location.reload();
    }
});