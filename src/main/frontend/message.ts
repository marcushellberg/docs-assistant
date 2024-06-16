export interface Message {
  role: 'assistant' | 'user';
  content: string;
}