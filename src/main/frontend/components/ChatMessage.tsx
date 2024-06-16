import {Message} from "Frontend/message";
import Markdown from "react-markdown";
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/atom-one-light.css';


interface MessageProps {
  message: Message
}

export default function ChatMessage({message}: MessageProps) {
  return (
    <div className="w-full mb-m">
      <div className="flex gap-m flex-col md:flex-row">
        <div className="text-2xl">{message.role === 'assistant' ? '🤖' : '🧑‍💻'}</div>
        <div className="max-w-full overflow-x-scroll">
          <Markdown
            rehypePlugins={[[rehypeHighlight, { ignoreMissing: true }]]}>
            {message.content}
          </Markdown>
        </div>
      </div>
    </div>
  );
}