import { MessageInput } from '@hilla/react-components/MessageInput';
import { useEffect, useState } from 'react';
import { DocsAssistantEndpoint } from 'Frontend/generated/endpoints.js';
import ChatCompletionMessage from 'Frontend/generated/com/example/application/service/openai/model/ChatCompletionMessage.js';
import Role from 'Frontend/generated/com/example/application/service/openai/model/ChatCompletionMessage/Role.js';
import ChatMessage from './ChatMessage.js';
import Framework from 'Frontend/generated/com/example/application/service/Framework.js';
import { Select, SelectItem } from '@hilla/react-components/Select';
import { VirtualList } from '@hilla/react-components/VirtualList';
import './main.css';

export default function App() {
  const [working, setWorking] = useState(false);
  const [framework, setFramework] = useState('');
  const [supportedFrameworks, setSupportedFrameworks] = useState<Framework[]>([]);
  const [messages, setMessages] = useState<ChatCompletionMessage[]>([]);

  useEffect(() => {
    DocsAssistantEndpoint.getSupportedFrameworks().then((supportedFrameworks) => {
      setSupportedFrameworks(supportedFrameworks);
      setFramework(supportedFrameworks[0].value!);
    });
  }, []);

  async function getCompletion(text: string) {
    if (working) return;
    setWorking(true);

    const messageHistory = [
      ...messages,
      {
        role: Role.USER,
        content: text,
      },
    ];

    // Display the question
    setMessages(messageHistory);

    // Add a new message to the list on the first response chunk, then append to it
    let firstChunk = true;

    function appendToLastMessage(chunk: string) {
      if (firstChunk) {
        // Init the response message on the first chunk
        setMessages((msg) => [
          ...msg,
          {
            role: Role.ASSISTANT,
            content: '',
          },
        ]);
        firstChunk = false;
      }

      setMessages((msg) => {
        const lastMessage = msg[msg.length - 1];
        lastMessage.content += chunk;
        return [...msg.slice(0, -1), lastMessage];
      });
    }

    // Get completion as stream
    DocsAssistantEndpoint.getCompletionStream(messageHistory, framework)
      .onNext((chunk) => appendToLastMessage(chunk))
      .onComplete(() => setWorking(false))
      .onError(() => {
        console.error('Error processing stream');
        setWorking(false);
      });
  }

  // Reset the messages when the framework changes
  function changeFramework(newFramework: string) {
    setFramework(newFramework);
    setMessages([]);
  }

  return (
    <div className="flex flex-col max-w-screen-lg mx-auto h-screen p-4 max-w">
      <div className="flex gap-4 mb-6 items-center justify-between">
        <h1 className="font-semibold text-lg md:text-2xl">Vaadin Docs Assistant</h1>
        <Select
          className="w-24 sm:w-48"
          items={supportedFrameworks as SelectItem[]}
          value={framework}
          onChange={(e) => changeFramework(e.target.value)}
        />
      </div>

      <VirtualList items={messages} className="flex-grow">
        {({ item }) => <ChatMessage content={item.content} role={item.role} />}
      </VirtualList>

      <MessageInput className="p-0 pt-2" onSubmit={(e) => getCompletion(e.detail.value)} disabled={working} />
    </div>
  );
}
