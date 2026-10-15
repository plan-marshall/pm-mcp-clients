import '@testing-library/jest-dom/vitest';
import { vi, beforeEach } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { messagesStore } from './lib/i18n';

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

function loadPropertiesFile(filePath: string): Record<string, string> {
  const content = fs.readFileSync(filePath, 'utf-8');
  const result: Record<string, string> = {};
  for (const line of content.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#') || trimmed.startsWith('!')) continue;
    const eqIdx = trimmed.indexOf('=');
    if (eqIdx > 0) {
      const key = trimmed.slice(0, eqIdx).trim();
      const value = trimmed.slice(eqIdx + 1).trim();
      result[key] = value;
    }
  }
  return result;
}

function loadAllEnglishMessages(): Record<string, string> {
  const messagesDir = path.resolve(__dirname, '../../pm-api/src/main/resources/messages');
  if (!fs.existsSync(messagesDir)) {
    return {};
  }
  const files = fs.readdirSync(messagesDir).filter((f) => f.endsWith('.en.properties'));
  const all: Record<string, string> = {};
  for (const file of files) {
    Object.assign(all, loadPropertiesFile(path.join(messagesDir, file)));
  }
  return all;
}

const defaultTestMessages: Record<string, string> = loadAllEnglishMessages();

beforeEach(() => {
  mockStorage.clear();
  messagesStore.set(defaultTestMessages);
  vi.restoreAllMocks();
});
