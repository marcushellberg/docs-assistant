import { createRoot } from 'react-dom/client';
import App from './App.js';

const root = createRoot(document.getElementById('outlet')!);
root.render(<App />);
