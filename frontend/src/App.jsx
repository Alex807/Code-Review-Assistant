import { useState, useRef } from 'react';
import Editor from '@monaco-editor/react';
import './App.css';

import hljs from 'highlight.js/lib/core';
import javascript from 'highlight.js/lib/languages/javascript';
import python from 'highlight.js/lib/languages/python';
import java from 'highlight.js/lib/languages/java';
import cpp from 'highlight.js/lib/languages/cpp';
import typescript from 'highlight.js/lib/languages/typescript';
import html from 'highlight.js/lib/languages/xml';
import css from 'highlight.js/lib/languages/css';
import axios from 'axios';

hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('python', python);
hljs.registerLanguage('java', java);
hljs.registerLanguage('cpp', cpp);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('html', html);
hljs.registerLanguage('css', css);

function App() {
  const [mode, setMode] = useState('manual');
  const [code, setCode] = useState('');
  const [detectedLanguage, setDetectedLanguage] = useState('');
  const [reviewResult, setReviewResult] = useState('');
  const [projectFiles, setProjectFiles] = useState([]); // { name, content, path }
  const editorRef = useRef(null);
  const folderInputRef = useRef(null);


  // === HEALTH CHECK ON MOUNT ===


  const checkHealth = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/review/health');
      if (res.ok) {
        setHealthStatus('up');
      } else {
        setHealthStatus('down');
      }
    } catch (err) {
      setHealthStatus('down');
    }
  };

  const detectLanguage = (content, fileName = '') => {
    const ext = fileName.split('.').pop()?.toLowerCase() || '';
    const extMap = {
      js: 'javascript', jsx: 'javascript', ts: 'typescript', py: 'python',
      java: 'java', cpp: 'cpp', c: 'c', html: 'html', css: 'css'
    };
    if (extMap[ext]) return extMap[ext];
    try {
      return hljs.highlightAuto(content).language || 'plaintext';
    } catch {
      return 'plaintext';
    }
  };

  // --- SINGLE / MULTIPLE FILE UPLOAD ---
  const handleFileChange = (e) => {
    const files = Array.from(e.target.files || []);
    processFiles(files);
  };

  // --- FOLDER UPLOAD (webkitDirectory) ---
  const handleFolderSelect = () => {
    folderInputRef.current?.click();
  };

  const handleFolderChange = (e) => {
    const files = Array.from(e.target.files || []);
    processFiles(files);
  };

  // --- CORE: Read files + build project structure ---
  const processFiles = (fileList) => {
    const entries = [];
    let combinedCode = '';

    fileList.forEach((file) => {
      const relativePath = file.webkitRelativePath || file.name;
      const reader = new FileReader();
      reader.onload = (ev) => {
        const content = ev.target?.result;
        entries.push({ name: file.name, path: relativePath, content });

        // Append to editor with clear file separators
        combinedCode += `\n\n// === ${relativePath} ===\n${content}`;
        setCode(combinedCode);

        const lang = detectLanguage(content, file.name);
        setDetectedLanguage((prev) => prev || lang); // keep first detected
        setProjectFiles([...entries]);
      };
      reader.readAsText(file);
    });
  };

  const handleEditorChange = (value) => {
    if (!value) return;
    setCode(value);
    const lang = detectLanguage(value);
    setDetectedLanguage(lang);

    if (editorRef.current) {
      const model = editorRef.current.getModel();
      if (model) {
        // @ts-ignore
        monaco.editor.setModelLanguage(model, lang);
      }
    }
  };

  const handleReview = async () => {
    if (!code.trim()) {
      alert('Please add code first.');
      return;
    }

    const lang = detectedLanguage || detectLanguage(code);
    setDetectedLanguage(lang);
    setReviewResult('Loading…');

    try {
      const res = await fetch('http://localhost:8080/api/review', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code, language: lang }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setReviewResult(data.review);
    } catch (err) {
      setReviewResult(`Error: ${err.message}`);
    }
  };

  return (
    <div className="app-container">
      <h1 className="title">AI-Powered Code Review Assistant</h1>

      <div className="mode-switcher">
        <button className={`mode-btn ${mode === 'manual' ? 'active' : ''}`} onClick={() => setMode('manual')}>
          Manual Input
        </button>
        <button className={`mode-btn ${mode === 'upload' ? 'active' : ''}`} onClick={() => setMode('upload')}>
          Upload Files/Folder
        </button>
      </div>

      {mode === 'manual' ? (
        <Editor
          height="400px"
          defaultLanguage="plaintext"
          value={code}
          onChange={handleEditorChange}
          onMount={(editor) => (editorRef.current = editor)}
          options={{ minimap: { enabled: false } }}
        />
      ) : (
        <div className="upload-section">
          <div className="upload-buttons">
            <label className="file-label">
              Choose Files
              <input type="file" multiple onChange={handleFileChange} style={{ display: 'none' }} />
            </label>

            <button className="folder-btn" onClick={handleFolderSelect}>
              Choose Folder
            </button>
            <input
              ref={folderInputRef}
              type="file"
              webkitdirectory="true"
              directory="true"
              style={{ display: 'none' }}
              onChange={handleFolderChange}
            />
          </div>

          {projectFiles.length > 0 && (
            <div className="file-tree">
              <strong>Project Files ({projectFiles.length})</strong>
              <ul>
                {projectFiles.map((f, i) => (
                  <li key={i} className="file-item">📄 {f.path}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      <button className="review-btn" onClick={handleReview}>
        Review Code
      </button>

      <button className="review-btn" onClick={checkHealth}>
        Health
      </button>

      {reviewResult && (
        <div className="result-box">
          <h2 className="result-title">Review Results</h2>
          <pre className="result-content">{reviewResult}</pre>
        </div>
      )}
    </div>
  );
}

export default App;