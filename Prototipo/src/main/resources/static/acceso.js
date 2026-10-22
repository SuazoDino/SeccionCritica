// Mostrar u ocultar la contraseña sin perder el foco del campo.
document.querySelectorAll('.ver-clave').forEach(button => {
  const input = document.getElementById(button.getAttribute('aria-controls'));
  button.addEventListener('click', () => {
    const visible = input.type === 'text';
    input.type = visible ? 'password' : 'text';
    button.textContent = visible ? 'Mostrar' : 'Ocultar';
    button.setAttribute('aria-pressed', String(!visible));
    input.focus();
  });
});

// Aviso de Bloq Mayús: la causa más común de "contraseña incorrecta".
const avisoMayus = document.querySelector('.aviso-mayus');
document.querySelectorAll('input[type="password"]').forEach(input => {
  ['keydown', 'keyup'].forEach(tipo => input.addEventListener(tipo, event => {
    if (avisoMayus && event.getModifierState) avisoMayus.hidden = !event.getModifierState('CapsLock');
  }));
});

// Evita envíos dobles por clics repetidos.
document.querySelectorAll('form.formulario').forEach(form => {
  form.addEventListener('submit', () => {
    const button = form.querySelector('button[type="submit"]');
    if (button) {
      button.disabled = true;
      button.textContent = 'Procesando…';
    }
  });
});

// Al volver con "atrás" el navegador puede restaurar la página con el botón bloqueado.
window.addEventListener('pageshow', event => {
  if (event.persisted) window.location.reload();
});
