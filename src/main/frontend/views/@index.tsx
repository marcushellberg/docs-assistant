import {Message} from "Frontend/message";
import {useEffect, useState} from "react";
import ChatMessage from "Frontend/components/ChatMessage";
import {Button, ComboBox, Icon, MessageInput} from "@vaadin/react-components";
import {DocsAssistantService} from "Frontend/generated/endpoints";
import {nanoid} from "nanoid";
import "@vaadin/icons";

export default function VaadinDocsAssistant() {
  const [working, setWorking] = useState(false);
  const [chatId, setChatId] = useState(nanoid());
  const [framework, setFramework] = useState('Flow');
  const [messages, setMessages] = useState<Message[]>([]);

  useEffect(() => {
    resetChat();
  }, [framework]);

  async function resetChat() {
    setMessages([]);
    await DocsAssistantService.clearChatMemory(chatId);
    setChatId(nanoid());
  }

  function appendToLastMessage(token: string) {
    setMessages(msgs => {
      const lastMessage = msgs[msgs.length - 1];
      lastMessage.content += token;
      return [...msgs.slice(0, -1), lastMessage];
    });
  }

  function getCompletion(message: string) {
    setWorking(true);
    setMessages(msgs => [...msgs, {role: 'user', content: message}]);

    let first = true;
    DocsAssistantService.chat(chatId, message, framework).onNext(token => {
      if (first && token) {
        setMessages(msgs => [...msgs, {role: 'assistant', content: token}]);
        first = false;
      } else {
        appendToLastMessage(token);
      }
    })
      .onError(() => setWorking(false))
      .onComplete(() => setWorking(false))
  }

  return (
    <div className="h-full p-m gap-m box-border flex flex-col">

      <div className="flex gap-m items-baseline">
        <h1 className="text-2xl flex-grow">Vaadin Docs Assistant</h1>
        <ComboBox
          value={framework}
          items={['Flow', 'Hilla']}
          style={{width: '8rem'}}
          onChange={(e) => setFramework(e.target.value)}/>
        <Button onClick={resetChat}><Icon icon="vaadin:refresh"/></Button>
      </div>

      <div className="flex-grow overflow-scroll">
        {messages.map((message, index) => <ChatMessage message={message} key={index}/>)}
      </div>

      <MessageInput
        disabled={working}
        className="p-0"
        onSubmit={e => getCompletion(e.detail.value)}/>
    </div>
  );
}
