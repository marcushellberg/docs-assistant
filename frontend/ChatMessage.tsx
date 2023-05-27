// components/ChatMessage.tsx
import React from 'react';
import ReactMarkdown from 'react-markdown';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/atom-one-light.css';
import ChatCompletionMessage from 'Frontend/generated/com/example/application/service/openai/model/ChatCompletionMessage.js';
import Role from 'Frontend/generated/com/example/application/service/openai/model/ChatCompletionMessage/Role.js';

export default function ChatMessage({ content, role }: ChatCompletionMessage) {
  return (
    <div className="w-full mb-4">
      <div className="flex flex-col md:flex-row md:gap-2">
        <div className="text-2xl">{role === Role.ASSISTANT ? '🤖' : '🧑‍💻'}</div>
        <div className="max-w-full overflow-x-scroll">
          <ReactMarkdown rehypePlugins={[[rehypeHighlight, { ignoreMissing: true }]]}>{content || ''}</ReactMarkdown>
        </div>
      </div>
    </div>
  );
}
