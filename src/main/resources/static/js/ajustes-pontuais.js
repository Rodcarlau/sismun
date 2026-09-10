document.addEventListener('DOMContentLoaded', () => {
  const detentoras = new Map([...document.querySelectorAll('#mapa-filtro-estoque option')].map(o => [o.value, o.dataset.detentora]));
  document.querySelectorAll('.estoque-select option[value]').forEach(o => { if (detentoras.has(o.value)) o.dataset.detentora = detentoras.get(o.value); });
  document.querySelectorAll('form[action$="/devolucao"]').forEach(form => {
    const accordion = form.closest('.accordion-body');
    const pendentes = new Set([...accordion.querySelectorAll('form[action$="/consumo"] input[name="itemDevolucaoId"]')].map(i => i.value));
    [...form.querySelectorAll('input[name="itemDevolucaoId"]')].forEach(id => {
      const bloco = id.closest('.mb-2');
      const quantidade = bloco.querySelector('input[name="quantidadeDevolvida"]');
      if (pendentes.has(id.value)) { bloco.hidden = true; bloco.querySelectorAll('input,select').forEach(c => c.disabled = true); }
      else if (quantidade) quantidade.value = quantidade.max || '0';
    });
    if (![...form.querySelectorAll('input[name="itemDevolucaoId"]')].some(i => !i.disabled)) form.hidden = true;
  });

  const titulo = [...document.querySelectorAll('h6')].find(h => h.textContent.trim() === 'TRANSPORTE E SEGURANÇA');
  if (titulo) {
    const campos = [titulo.parentElement.nextElementSibling, titulo.parentElement.nextElementSibling?.nextElementSibling, titulo.parentElement.nextElementSibling?.nextElementSibling?.nextElementSibling].filter(Boolean);
    const ativo = campos.some(c => [...c.querySelectorAll('option:checked')].length);
    const chave = document.createElement('input'); chave.type='checkbox'; chave.className='form-check-input ms-2'; chave.checked=ativo;
    const rotulo = document.createElement('label'); rotulo.className='form-check-label ms-2'; rotulo.textContent='Habilitar'; rotulo.prepend(chave); titulo.append(rotulo);
    const alternar=()=>campos.forEach(c=>{c.hidden=!chave.checked;c.querySelectorAll('select').forEach(s=>s.disabled=!chave.checked)}); chave.addEventListener('change',alternar); alternar();
  }

  const formulario = document.querySelector('form[action$="/movimentacoes"]');
  if (formulario) {
    const linha=(nomes,larguras)=>{const blocos=nomes.map(n=>formulario.querySelector(`[name="${n}"]`)?.closest('div')).filter(Boolean);if(!blocos.length)return;const caixa=document.createElement('div');caixa.className='col-12 row g-3';blocos[0].before(caixa);blocos.forEach((b,i)=>{b.className=larguras[i];caixa.appendChild(b)})};
    linha(['eb','diex','dataSolicitacao','dataApanha'],['col-md-3','col-md-3','col-md-3','col-md-3']);
    linha(['omSolicitante','oficialMunicaoSolicitante','oficialMunicaoOmDetentora'],['col-md-4','col-md-4','col-md-4']);
    linha(['paiolOrigem','militarPaiolRetirada','paiolDestino','militarPaiolRecebimento'],['col-md-3','col-md-3','col-md-3','col-md-3']);
    setTimeout(()=>document.querySelectorAll('.item-municao').forEach(i=>{const p=i.querySelector('.paiol-item-select')?.parentElement,m=i.querySelector('.municao-select')?.parentElement,q=i.querySelector('[name="quantidadeItem"]')?.parentElement,a=i.querySelector('.remover-item')?.parentElement;if(p)p.className='col-md-2';if(m)m.className='col-md-3';if(q)q.className='col-md-2';if(a)a.className='col-md-1'}),100);
  }
});
