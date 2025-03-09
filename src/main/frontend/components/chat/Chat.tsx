import ChatMessage from './ChatMessage.js';
import { Button, Icon, Scroller, TextArea } from '@vaadin/react-components';
import './Chat.css';
import Dropzone from 'dropzone';
import 'dropzone/dist/basic.css';
import React, { useEffect, useRef, useState } from 'react';

export interface Subscription<T> {
  onNext(callback: (value: T) => void): Subscription<T>;
  onError(callback: (message: string) => void): Subscription<T>;
  onComplete(callback: () => void): Subscription<T>;
}

export interface Message {
  role?: string;
  content?: string;
  attachments?: Array<Attachment | undefined>;
}

interface Attachment {
  type?: string;
  key?: string;
  fileName?: string;
  url?: string;
}

export interface AiChatService<T> {
  stream(chatId: string, userMessage: string, options?: T): Subscription<string>;

  getHistory(chatId: string): Promise<Message[]>;

  closeChat(chatId: string): Promise<void>;

  uploadAttachment(chatId: string, file: File): Promise<string>;

  removeAttachment(chatId: string, attachmentId: string): Promise<void>;
}

// --

interface ChatOptions {
  systemMessage: string;
}

interface ChatProps<T> {
  chatId: string;
  service: AiChatService<T>;
  acceptedFiles?: string;
  options?: T;
  renderer?: Parameters<typeof ChatMessage>[0]['renderer'];
  className?: string;
}

export function Chat<T = {}>({ chatId, service, acceptedFiles, options, renderer, className }: ChatProps<T>) {
  const [working, setWorking] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [message, setMessage] = useState('');
  const dropzone = useRef<Dropzone>();

  useEffect(() => {
    service.getHistory(chatId).then(setMessages);
    return () => {
      // Close the previous chat when a new one is started
      service.closeChat(chatId);
    };
  }, [chatId]);

  function appendToLastMessage(token: string) {
    setMessages((msgs) => {
      const lastMessage = msgs[msgs.length - 1];
      lastMessage.content += token;
      return [...msgs.slice(0, -1), lastMessage];
    });
  }

  async function addAttachment(file: File) {
    const attachmentId = await service.uploadAttachment(chatId, file);
    (file as any).__attachmentId = attachmentId;
  }

  async function removeAttachment(file: File) {
    await service.removeAttachment(chatId, (file as any).__attachmentId);
  }

  function getCompletion(userMessage: string, attachments?: File[]) {
    setWorking(true);

    const uploadedAttachments = (attachments || []).filter((file) => '__attachmentId' in file);

    const messageAttachments: Attachment[] = uploadedAttachments.map((file) => {
      const isImage = file.type.startsWith('image/');
      return {
        key: file.__attachmentId as string,
        fileName: file.name,
        type: isImage ? 'image' : 'document',
        url: isImage ? (file as any).dataURL : undefined,
      };
    });

    setMessages((msgs) => [...msgs, { role: 'user', content: userMessage, attachments: messageAttachments }]);

    let first = true;
    service
      .stream(chatId, userMessage, options)
      .onNext((token) => {
        if (first && token) {
          setMessages((msgs) => [...msgs, { role: 'assistant', content: token }]);
          first = false;
        } else {
          appendToLastMessage(token);
        }
      })
      .onError(() => setWorking(false))
      .onComplete(() => setWorking(false));
  }

  function onSubmit() {
    getCompletion(
      message,
      dropzone.current?.files.filter((file) => file.accepted),
    );
    setMessage('');

    if (dropzone.current) {
      Object.assign(dropzone.current, { ignoreRemove: true });
      dropzone.current.removeAllFiles();
      Object.assign(dropzone.current, { ignoreRemove: false });
    }
  }

  useEffect(() => {
    if (acceptedFiles) {
      dropzone.current = new Dropzone('.vaadin-chat-component', {
        url: '/file/post',
        previewsContainer: '.dropzone-previews',
        autoProcessQueue: false,
        addRemoveLinks: true,
        acceptedFiles,
        maxFilesize: 5,
      });

      dropzone.current.on('addedfile', (file) => addAttachment(file));
      dropzone.current.on('removedfile', (file) => {
        // TODO: Is there a better way to handle this?
        if (!('ignoreRemove' in dropzone.current!)) {
          removeAttachment(file);
        }
      });
    }

    return () => {
      dropzone.current?.destroy();
    };
  }, []);

  const waiting = messages[messages.length - 1]?.role === 'user';

  return (
    <div className={`vaadin-chat-component dropzone ${className}`}>
      <Scroller className="flex-grow" aria-label="Chat messages">
        {messages.map((message, index) => (
          <ChatMessage message={message} key={index} renderer={renderer} />
        ))}
        {waiting ? <ChatMessage waiting message={{ role: 'assistant', content: '' }} /> : null}
      </Scroller>

      {waiting ? (
        <style>
          {`
            .v-loading-indicator {
              display: none !important;
            }
          `}
        </style>
      ) : null}

      <div className="input-container p-s" aria-label="Input container">
        <div className="dropzone-previews" dangerouslySetInnerHTML={{ __html: '' }}></div>

        <TextArea
          className="input"
          minRows={1}
          disabled={working}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey && message) {
              e.preventDefault();
              onSubmit();
            }
          }}
          onValueChanged={(e) => setMessage(e.detail.value)}
          placeholder="Message"
          value={message}
          aria-label="Message input">
          <Button
            theme="icon tertiary small"
            className="dz-message"
            slot="suffix"
            disabled={working}
            hidden={!acceptedFiles}
            aria-label="Upload attachment">
            <Icon icon="vaadin:upload" />
          </Button>

          <Button
            theme="icon tertiary small"
            slot="suffix"
            onClick={onSubmit}
            disabled={working || !message}
            aria-label="Send">
            <Icon
              src={`data:image/svg+xml,${encodeURIComponent(
                `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
                </svg>`,
              )}`}
            />
          </Button>
        </TextArea>
      </div>

      <div className="drop-curtain">Drop a file here to add it to the chat 📁</div>
    </div>
  );
}
