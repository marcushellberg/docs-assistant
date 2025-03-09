import {useCallback, useEffect, useState} from 'react';
import {Chat} from '@vaadin/flow-frontend/chat/Chat.js';
import {Button, Icon, Select, Tooltip} from '@vaadin/react-components';
import {nanoid} from 'nanoid';
import '@vaadin/icons';
import '@vaadin/vaadin-lumo-styles/icons';
import './index.css';
import Mermaid from 'Frontend/components/Mermaid.js';
import {useForm} from '@vaadin/hilla-react-form';
import {DocsAssistantService} from "Frontend/generated/endpoints";
import ChatOptions from "Frontend/generated/org/vaadin/marcus/docsassistant/client/DocsAssistantService/ChatOptions";
import ChatOptionsModel
  from "Frontend/generated/org/vaadin/marcus/docsassistant/client/DocsAssistantService/ChatOptionsModel";

const defaultOptions: ChatOptions = {
  framework: 'flow'
};

const availableFrameworks = [
  {
    label: 'Flow',
    value: 'flow'
  },
  {
    label: 'Hilla',
    value: 'hilla'
  }
]

export default function SpringAiAssistant() {
  const [chatId, setChatId] = useState(nanoid());

  async function resetChat() {
    setChatId(nanoid());
  }

  function clearChatHistoryFromServer() {
    DocsAssistantService.closeChat(chatId);
  }

  // Set up form for managing chat options
  const {field, model, read, value} = useForm(ChatOptionsModel);

  useEffect(() => {
    clearChatHistoryFromServer();
    resetChat();
  }, [value.framework]);


  // On attach, read in the default options. On detach, clear chat from server.
  useEffect(() => {
    read(defaultOptions);
    return () => clearChatHistoryFromServer();
  }, []);

  // Define a custom renderer for Mermaid charts
  const renderer = useCallback((language = '', content = '') => {
    if (language.includes('mermaid')) {
      return <Mermaid chart={content}/>;
    }
    return null;
  }, []);

  // @ts-ignore
  return (
    <div className="main-layout">
      <div className="chat-layout">
        <header className="chat-header">
          <h1 className="chat-heading">
            <span><Icon icon="vaadin:vaadin-v"/></span>
            <span>Vaadin Docs Assistant</span>
          </h1>

          <Select items={availableFrameworks} {...field(model.framework)}/>

          <Button onClick={resetChat} theme="icon small contrast tertiary">
            <Icon icon="lumo:reload"/>
            <Tooltip slot="tooltip" text="New chat"/>
          </Button>
        </header>

        <Chat
          chatId={chatId}
          service={DocsAssistantService}
          options={value}
          renderer={renderer}
        />
      </div>
    </div>
  );
}
