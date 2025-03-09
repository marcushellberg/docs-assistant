import Markdown, { type Components } from 'react-markdown';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/atom-one-light.css';
import React, { ReactNode, useMemo } from 'react';

interface Props {
  content: string;
  renderer?: (language: string, content: string) => ReactNode;
}

export default function ({ content, renderer }: Props) {
  const components: Components = useMemo(
    () => ({
      code: ({ className, children, ...props }) => {
        if (className && typeof children === 'string' && renderer) {
          const language = className
            .split(' ')
            .find((c) => c.startsWith('language-'))
            ?.replace('language-', '');

          if (language) {
            try {
              const result = renderer(language, children);
              if (result) {
                return result;
              }
            } catch {
              // Rendering may fail with incomplete data
            }
          }
        }

        return (
          <code className={className} {...props}>
            {children}
          </code>
        );
      },
    }),
    [renderer],
  );

  return (
    <Markdown rehypePlugins={[[rehypeHighlight, { ignoreMissing: true }]]} components={components}>
      {content}
    </Markdown>
  );
}
