import './styles/base.css';
import { mount } from 'svelte';
import App from './App.svelte';

const target = document.getElementById('app');

if (!target) {
  throw new Error('Target root element #app not found');
}

const app = mount(App, {
  target,
});

export default app;
