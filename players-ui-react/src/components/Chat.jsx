import React, { useState } from 'react';

function Chat() {
    const [prompt, setPrompt] = useState('');
    const [messages, setMessages] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleSend = () => {
        if (!prompt.trim()) return;

        const userMessage = prompt;
        setMessages(prev => [...prev, { role: 'user', text: userMessage }]);
        setPrompt('');
        setLoading(true);

        fetch('/v1/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt: userMessage })
        })
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.text();
            })
            .then(response => {
                setMessages(prev => [...prev, { role: 'assistant', text: response }]);
            })
            .catch(err => {
                setMessages(prev => [...prev, { role: 'assistant', text: `Error: ${err.message}` }]);
            })
            .finally(() => setLoading(false));
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !loading) handleSend();
    };

    return (
        <div style={{ padding: '20px', maxWidth: '600px', margin: '0 auto' }}>
            <h3>Player Chat</h3>

            <div style={{
                border: '1px solid #ccc',
                borderRadius: '8px',
                padding: '10px',
                height: '400px',
                overflowY: 'auto',
                marginBottom: '10px',
                backgroundColor: '#f9f9f9'
            }}>
                {messages.length === 0 && (
                    <p style={{ color: '#999' }}>Ask me about a player...</p>
                )}
                {messages.map((msg, i) => (
                    <div key={i} style={{
                        marginBottom: '10px',
                        textAlign: msg.role === 'user' ? 'right' : 'left'
                    }}>
                        <span style={{
                            display: 'inline-block',
                            padding: '8px 12px',
                            borderRadius: '12px',
                            backgroundColor: msg.role === 'user' ? '#007bff' : '#e9e9e9',
                            color: msg.role === 'user' ? '#fff' : '#333',
                            maxWidth: '80%',
                            whiteSpace: 'pre-wrap',
                            textAlign: 'left'
                        }}>
                            {msg.text}
                        </span>
                    </div>
                ))}
                {loading && (
                    <div style={{ color: '#999', fontStyle: 'italic' }}>Thinking...</div>
                )}
            </div>

            <div style={{ display: 'flex', gap: '8px' }}>
                <input
                    value={prompt}
                    onChange={(e) => setPrompt(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="Ask about a player..."
                    disabled={loading}
                    style={{
                        flex: 1,
                        padding: '8px 12px',
                        borderRadius: '4px',
                        border: '1px solid #ccc'
                    }}
                />
                <button
                    onClick={handleSend}
                    disabled={loading || !prompt.trim()}
                    style={{
                        padding: '8px 16px',
                        borderRadius: '4px',
                        border: 'none',
                        backgroundColor: '#007bff',
                        color: '#fff',
                        cursor: 'pointer'
                    }}
                >
                    Send
                </button>
            </div>
        </div>
    );
}

export default Chat;
