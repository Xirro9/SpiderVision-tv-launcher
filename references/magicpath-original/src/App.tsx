import { Theme } from './settings/types';
import { OrreryComicEdition } from './components/generated/OrreryComicEdition';

let theme: Theme = 'light';

function App() {
  function setTheme(theme: Theme) {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  setTheme(theme);

  return (
    <>
      <OrreryComicEdition />
    </>
  ); // %EXPORT_STATEMENT%
}

export default App;
