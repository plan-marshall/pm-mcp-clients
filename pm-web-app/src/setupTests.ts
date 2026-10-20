import '@testing-library/jest-dom/vitest';
import { vi, beforeEach } from 'vitest';
import { messagesStore } from './lib/i18n';
import { loadMessages } from './test/catalogue-fixture';

class LocalStorageMock implements Storage {
  private store: Record<string, string> = {};

  clear(): void {
    this.store = {};
  }

  getItem(key: string): string | null {
    return this.store[key] ?? null;
  }

  setItem(key: string, value: string): void {
    this.store[key] = String(value);
  }

  removeItem(key: string): void {
    delete this.store[key];
  }

  get length(): number {
    return Object.keys(this.store).length;
  }

  key(index: number): string | null {
    return Object.keys(this.store)[index] ?? null;
  }
}

const mockStorage = new LocalStorageMock();

Object.defineProperty(globalThis, 'localStorage', {
  value: mockStorage,
  writable: true,
  configurable: true,
});

const defaultTestMessages: Record<string, string> = loadMessages('en');

beforeEach(() => {
  mockStorage.clear();
  messagesStore.set(defaultTestMessages);
  vi.restoreAllMocks();
});
