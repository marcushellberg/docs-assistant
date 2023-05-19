import {MessageInput} from "@hilla/react-components/MessageInput";
import {useEffect, useState} from "react";
import {DocsAssistantEndpoint} from "Frontend/generated/endpoints.js";
import ChatCompletionMessage
  from "Frontend/generated/com/example/application/service/openai/model/ChatCompletionMessage.js";
import Role from "Frontend/generated/com/example/application/service/openai/model/ChatCompletionMessage/Role.js";
import ChatMessage from "./ChatMessage.js";
import Framework from "Frontend/generated/com/example/application/service/Framework.js";
import {Select, SelectItem} from "@hilla/react-components/Select";
import {VirtualList} from "@hilla/react-components/VirtualList";

export default function App() {
  const [working, setWorking] = useState(false);
  const [framework, setFramework] = useState('');
  const [supportedFrameworks, setSupportedFrameworks] = useState<Framework[]>([]);
  const [messages, setMessages] = useState<ChatCompletionMessage[]>([]);


  useEffect(() => {
    DocsAssistantEndpoint.getSupportedFrameworks().then(supportedFrameworks => {
      setSupportedFrameworks(supportedFrameworks);
      setFramework(supportedFrameworks[0].value!);
    });
  }, []);

  async function getCompletion(text: string) {
    const messageHistory = [...messages, {
      role: Role.USER,
      content: text
    }];

    // Display the question
    setMessages(messageHistory);

    // Add a new message to the list on the first response chunk, then append to it
    let first = true;
    function appendToLastMessage(chunk: string) {
      if(first) {
        // Init the response message on the first chunk
        setMessages(msg => [...msg, {
          role: Role.ASSISTANT,
          content: ''
        }]);
        first = false;
      }

      setMessages(msg => {
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
    <div className="flex flex-col gap-s h-full p-m box-border max-w-screen-lg m-auto">

      <div className="flex gap-m mb-xl items-center justify-between">
        <h1>Vaadin Docs Assistant</h1>
        <Select items={supportedFrameworks as SelectItem[]} value={framework} onChange={e => changeFramework(e.target.value)} />
      </div>

      <VirtualList items={messages} className="flex-grow">
        {({item}) => <ChatMessage content={item.content} role={item.role}/> }
      </VirtualList>

      <MessageInput onSubmit={e => getCompletion(e.detail.value)} />
    </div>
  );
}
