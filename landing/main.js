const drill = document.getElementById('drill');
const gap = document.getElementById('gap');
const result = document.getElementById('result');
const checkButton = document.getElementById('check');
const againButton = document.getElementById('again');

const normalize = text => text.trim().replace(/\s+/g, ' ').toLowerCase();

function showResult(isCorrect) {
  gap.classList.toggle('is-ok', isCorrect);
  gap.classList.toggle('is-bad', !isCorrect);
  result.className = 'drill-result ' + (isCorrect ? 'is-ok' : 'is-bad');
  result.innerHTML = isCorrect
    ? '<b>Correct</b>In the app, the next exercise comes up now.'
    : '<b>Correct answer:</b><span class="en">Where <strong>were you</strong> born?</span>';
  gap.readOnly = true;
  checkButton.hidden = true;
  againButton.hidden = false;
  againButton.focus();
}

drill.addEventListener('submit', event => {
  event.preventDefault();
  if (!gap.value.trim()) {
    gap.focus();
    return;
  }
  showResult(normalize(gap.value) === 'were you');
});

againButton.addEventListener('click', () => {
  gap.value = '';
  gap.readOnly = false;
  gap.classList.remove('is-ok', 'is-bad');
  result.className = 'drill-result';
  result.textContent = '';
  againButton.hidden = true;
  checkButton.hidden = false;
  gap.focus();
});
