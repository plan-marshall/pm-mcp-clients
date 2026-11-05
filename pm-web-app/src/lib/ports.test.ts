import { describe, it, expect } from 'vitest';
import { resolvePorts, holdsAssignedPort } from './ports';

describe('resolvePorts', () => {
  it('returns default ports 5173 and 7420 when environment variables are unset', () => {
    const ports = resolvePorts({});
    expect(ports.webPort).toBe(5173);
    expect(ports.mockWebPort).toBe(7420);
    expect(ports.mockTarget).toBe('http://127.0.0.1:7420');
  });

  it('reads PM_WEB_PORT and PM_MOCK_WEB_PORT from environment', () => {
    const ports = resolvePorts({
      PM_WEB_PORT: '20761',
      PM_MOCK_WEB_PORT: '20760',
    });
    expect(ports.webPort).toBe(20761);
    expect(ports.mockWebPort).toBe(20760);
    expect(ports.mockTarget).toBe('http://127.0.0.1:20760');
  });

  it('throws an error if PM_WEB_PORT is invalid', () => {
    expect(() => resolvePorts({ PM_WEB_PORT: 'invalid' })).toThrow(/Invalid PM_WEB_PORT/);
    expect(() => resolvePorts({ PM_WEB_PORT: '-5' })).toThrow(/Invalid PM_WEB_PORT/);
    expect(() => resolvePorts({ PM_WEB_PORT: '0' })).toThrow(/Invalid PM_WEB_PORT/);
    expect(() => resolvePorts({ PM_WEB_PORT: '70000' })).toThrow(/Invalid PM_WEB_PORT/);
  });

  it('throws an error if PM_MOCK_WEB_PORT is invalid', () => {
    expect(() => resolvePorts({ PM_MOCK_WEB_PORT: 'invalid' })).toThrow(/Invalid PM_MOCK_WEB_PORT/);
    expect(() => resolvePorts({ PM_MOCK_WEB_PORT: '-1' })).toThrow(/Invalid PM_MOCK_WEB_PORT/);
    expect(() => resolvePorts({ PM_MOCK_WEB_PORT: '99999' })).toThrow(/Invalid PM_MOCK_WEB_PORT/);
  });
});

describe('holdsAssignedPort', () => {
  it('holds a port the environment assigned', () => {
    expect(holdsAssignedPort({ PM_WEB_PORT: '20931' })).toBe(true);
  });

  it('lets the port move when none was assigned', () => {
    expect(holdsAssignedPort({})).toBe(false);
    expect(holdsAssignedPort({ PM_WEB_PORT: '' })).toBe(false);
    expect(holdsAssignedPort({ PM_WEB_PORT: '   ' })).toBe(false);
  });
});
