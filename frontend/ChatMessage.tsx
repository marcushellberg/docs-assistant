// components/ChatMessage.tsx
import React from 'react';
import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import 'highlight.js/styles/atom-one-light.css'
import ChatCompletionMessage
  from "Frontend/generated/com/example/application/service/openai/model/ChatCompletionMessage.js";
import Role from "Frontend/generated/com/example/application/service/openai/model/ChatCompletionMessage/Role.js";


export default function ChatMessage({content, role}: ChatCompletionMessage) {
  return (
    <div className={`w-full mb-l ${
      role === Role.USER ? 'bg-white' : ''
    }`}>
      <div className="flex gap-m ">
        <div className="text-2xl">
          {role === Role.ASSISTANT ? '🤖' : '🧑‍💻'}
        </div>
        <div className="max-w-full overflow-x-scroll">
          <ReactMarkdown rehypePlugins={[[rehypeHighlight, {ignoreMissing: true}]]}>
            {content || ''}
          </ReactMarkdown>
        </div>
      </div>
    </div>
  );
}

