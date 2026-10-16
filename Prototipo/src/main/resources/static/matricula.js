const errorDialog = document.getElementById('error-matricula');
if (errorDialog) errorDialog.showModal();

// Los eventos que se superponen se distribuyen en carriles sin ocultarse.
document.querySelectorAll('.columna-dia').forEach(day => {
  const events = [...day.querySelectorAll('.evento')].sort((a,b) => +a.dataset.inicio - +b.dataset.inicio);
  let group = [], end = -1;
  function layout() {
    const lanes = [];
    for (const event of group) {
      let lane = lanes.findIndex(until => until <= +event.dataset.inicio);
      if (lane < 0) lane = lanes.length;
      lanes[lane] = +event.dataset.fin;
      event.dataset.lane = lane;
    }
    for (const event of group) {
      event.style.left = `calc(${100 * +event.dataset.lane / lanes.length}% + 3px)`;
      event.style.width = `calc(${100 / lanes.length}% - 6px)`;
    }
  }
  for (const event of events) {
    if (+event.dataset.inicio >= end) { layout(); group = []; end = -1; }
    group.push(event); end = Math.max(end, +event.dataset.fin);
  }
  layout();
});
const daySelect = document.getElementById('dia-agenda');
if (daySelect) {
  const mobile = window.matchMedia('(max-width:700px)');
  function showDay() {
    document.querySelectorAll('.agenda [data-dia]').forEach(column => {
      column.classList.toggle('dia-oculto', mobile.matches && column.dataset.dia !== daySelect.value);
    });
  }
  daySelect.addEventListener('change', showDay);
  mobile.addEventListener('change', showDay);
  showDay();
}
