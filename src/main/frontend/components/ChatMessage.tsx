import { Message } from "Frontend/message";
import Markdown from "react-markdown";
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/atom-one-light.css';
import { Icon } from "@vaadin/react-components";


interface MessageProps {
  message: Message
}

export default function ChatMessage({ message }: MessageProps) {
  return (
    <div className={'flex flex-col sm:flex-row gap-m p-m mt-m' + (message.role !== 'assistant' ? ' me' : '')}>
      <Icon icon="vaadin:vaadin-h" className="flex-none rounded-full w-m h-m p-s border text-primary" hidden={message.role !== 'assistant'} />
      <div className="message-content">
        <Markdown
          rehypePlugins={[[rehypeHighlight, { ignoreMissing: true }]]}>
          {message.content}
        </Markdown>
      </div>
    </div>
  );
}
