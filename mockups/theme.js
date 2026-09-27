// Тема макетов: ?theme=dark|light в адресе или выбор в Settings (localStorage). System — как у системы.
(function () {
  let t = new URLSearchParams(location.search).get('theme');
  if (!t) try { t = localStorage.getItem('ef-theme'); } catch (e) {}
  window.applyTheme = function (theme) {
    const dark = theme === 'dark' || (theme !== 'light' && matchMedia('(prefers-color-scheme: dark)').matches);
    document.documentElement.classList.toggle('dark', dark);
  };
  applyTheme(t || 'system');
})();
