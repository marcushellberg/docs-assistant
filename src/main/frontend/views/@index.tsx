import { Message } from "Frontend/message";
import { useEffect, useRef, useState } from "react";
import ChatMessage from "Frontend/components/ChatMessage";
import { Button, Icon, MessageInput, Scroller, ScrollerElement, Select, Tooltip } from "@vaadin/react-components";
import { DocsAssistantService } from "Frontend/generated/endpoints";
import { nanoid } from "nanoid";
import "@vaadin/icons";
import "@vaadin/vaadin-lumo-styles/icons";

export default function VaadinDocsAssistant() {
  const [working, setWorking] = useState(false);
  const [chatId, setChatId] = useState(nanoid());
  const [framework, setFramework] = useState('Flow');
  const [messages, setMessages] = useState<Message[]>([]);
  const scroller = useRef<ScrollerElement>(null);
  const scrollInterval = useRef<number | undefined>();

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

  function scrollToEnd() {
    if (scroller.current && scroller.current.scrollTop < (scroller.current.scrollHeight - scroller.current.offsetHeight - 10)) {
      scroller.current?.children[scroller.current.childElementCount - 1]?.scrollIntoView({ block: 'end' });
    }
  }

  function enableScrollTracking() {
    disableScrollTracking();
    scrollInterval.current = window.setInterval(scrollToEnd, 100);
  }

  function disableScrollTracking() {
    window.clearInterval(scrollInterval.current);
  }

  useEffect(() => {
    if (working) {
      enableScrollTracking();
    } else {
      disableScrollTracking();
    }
  }, [working]);

  useEffect(() => {
    if (scroller.current) {
      scroller.current.onscroll = () => {
        disableScrollTracking();
        if (scroller.current) {
          if (scroller.current.scrollTop > (scroller.current.scrollHeight - scroller.current.offsetHeight - 50)) {
            enableScrollTracking();
          }
        }
      };
    }
  }, [scroller]);

  function getCompletion(message: string) {
    setWorking(true);
    setMessages(msgs => [...msgs, { role: 'user', content: message }]);

    let first = true;
    DocsAssistantService.chat(chatId, message, framework).onNext(token => {
      if (first && token) {
        setMessages(msgs => [...msgs, { role: 'assistant', content: token }]);
        first = false;
      } else {
        appendToLastMessage(token);
      }
    })
      .onError(() => setWorking(false))
      .onComplete(() => setWorking(false))
  }

  useEffect(() => {
    function onVisualViewportChange() {
      const visualViewportHeight = window.visualViewport!.height;
      if (visualViewportHeight < document.documentElement.clientHeight) {
        document.documentElement.style.setProperty('--viewport-height', `${visualViewportHeight}px`);
      } else {
        document.documentElement.style.removeProperty('--viewport-height');
      }
    }

    window.visualViewport?.addEventListener('resize', onVisualViewportChange);

    return () =>
      window.visualViewport?.removeEventListener('resize', onVisualViewportChange);
  }, []);

  return (
    <div className="main-layout flex flex-col">

      <header className="flex gap-s items-center px-m">
        <h1 className="text-l flex-grow flex items-center gap-m">
          <Icon icon="vaadin:vaadin-h" style={{ width: '1em', color: 'var(--lumo-primary-color)' }} aria-label="Vaadin" />
          <span>Docs Assistant</span>
        </h1>
        <Select
          className="framework-select"
          value={framework}
          items={[{ label: 'Flow' }, { label: 'Hilla' }]}
          onChange={(e) => setFramework(e.target.value)} />
        <Button onClick={resetChat} theme="icon small contrast tertiary">
          <Icon icon="lumo:reload" />
          <Tooltip slot="tooltip" text="Clear chat" />
        </Button>
      </header>

      <Scroller className="flex-grow" theme="overflow-indicators" ref={scroller}>
        {messages.map((message, index) => <ChatMessage message={message} key={index} />)}
      </Scroller>

      <MessageInput
        disabled={working}
        className="p-s"
        /* TODO need to pass this in here, because Vite throws an error if url() is used in styles.css or index.html */
        style={{ '--mask-image': 'url(\'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="black"><path stroke-linecap="round" stroke-linejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" /></svg>\')' }}
        onSubmit={e => getCompletion(e.detail.value)} />
    </div>
  );
}
