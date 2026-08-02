import { useState } from 'react'
import api, { apiMessage } from '@/shared/lib/api'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { Button, Field, Input, Textarea } from '@/shared/ui'
import Modal from '@/shared/components/Modal'
import { Plus, Trash2 } from 'lucide-react'

// Bac si ghi ho so kham + don thuoc cho 1 lich hen (dung o Lich kham + Lich su)
export default function RecordModal({ appointment, onClose, onSaved, toast }) {
  const { t } = useI18n()
  const [diagnosis, setDiagnosis] = useState('')
  const [notes, setNotes] = useState('')
  const [items, setItems] = useState([{ medicineName: '', dosage: '', quantity: 1, instruction: '' }])
  const [saving, setSaving] = useState(false)

  const setItem = (i, k) => (e) =>
    setItems((arr) => arr.map((it, idx) => (idx === i ? { ...it, [k]: k === 'quantity' ? +e.target.value : e.target.value } : it)))
  const addItem = () => setItems((a) => [...a, { medicineName: '', dosage: '', quantity: 1, instruction: '' }])
  const removeItem = (i) => setItems((a) => a.filter((_, idx) => idx !== i))

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await api.post('/medical-records', {
        appointmentId: appointment.id,
        diagnosis,
        notes,
        prescriptionItems: items.filter((it) => it.medicineName.trim()),
      })
      toast.success(t('record.saved'))
      onClose()
      onSaved()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={t('record.title')}>
      <form onSubmit={submit} className="space-y-4">
        <Field label={t('record.diagnosis')} required>
          <Textarea value={diagnosis} onChange={(e) => setDiagnosis(e.target.value)} required placeholder={t('record.diagnosisPlaceholder')} />
        </Field>
        <Field label={t('record.notes')}>
          <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} placeholder={t('record.notesPlaceholder')} />
        </Field>
        <div>
          <div className="mb-2 flex items-center justify-between">
            <span className="text-sm font-medium text-text">{t('record.prescription')}</span>
            <Button type="button" variant="ghost" onClick={addItem} className="text-primary">
              <Plus className="h-4 w-4" /> {t('record.addMedicine')}
            </Button>
          </div>
          <div className="space-y-2">
            {items.map((it, i) => (
              <div key={i} className="grid grid-cols-12 gap-2">
                <Input className="col-span-4" placeholder={t('record.medicineName')} value={it.medicineName} onChange={setItem(i, 'medicineName')} />
                <Input className="col-span-3" placeholder={t('record.dose')} value={it.dosage} onChange={setItem(i, 'dosage')} />
                <Input className="col-span-2" type="number" min="1" value={it.quantity} onChange={setItem(i, 'quantity')} aria-label={t('record.qtyAria')} />
                <Input className="col-span-2" placeholder={t('record.usage')} value={it.instruction} onChange={setItem(i, 'instruction')} />
                <button type="button" onClick={() => removeItem(i)} className="col-span-1 flex items-center justify-center text-slate-400 hover:text-danger" aria-label={t('record.removeAria')}>
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>{t('common.cancel')}</Button>
          <Button type="submit" loading={saving}>{t('record.save')}</Button>
        </div>
      </form>
    </Modal>
  )
}
