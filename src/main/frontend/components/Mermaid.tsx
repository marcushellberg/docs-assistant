import { useEffect, useState } from 'react';
import mermaid from 'mermaid';
import { nanoid } from 'nanoid';

interface MermaidProps {
  chart: string;
}

const Mermaid = ({ chart }: MermaidProps) => {
  const [currentSvg, setCurrentSvg] = useState('');

  useEffect(() => {
    mermaid.initialize({
      startOnLoad: false,
      suppressErrorRendering: true,
    });
  }, []);

  const mermaidUpdate = async () => {
    try {
      const result = await mermaid.render(nanoid(), chart);
      setCurrentSvg(result.svg);
    } catch (e) {
      // Ignore
    }
  };

  useEffect(() => {
    mermaidUpdate();
  }, [chart]);

  return <div className="mermaid" dangerouslySetInnerHTML={{ __html: currentSvg }}></div>;
};

export default Mermaid;
