import { useEffect, useState } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { useToast } from '@/shared/components/Toast'

/**
 * Gom boilerplate cho trang list phan trang phia SERVER (endpoint tra PageResponse).
 * - `params`: query gui kem (derive tu `deps`).
 * - `deps`: cac gia tri filter -> doi thi ve trang 0 va fetch lai.
 * Tra ve { items, setItems, page, setPage, totalPages, totalElements, loading, reload }.
 */
export function usePaginatedList(path, { params = {}, pageSize = 10, deps = [] } = {}) {
  const toast = useToast()
  const [items, setItems] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [refreshKey, setRefreshKey] = useState(0)

  // Doi filter -> ve trang dau
  useEffect(() => { setPage(0) }, deps) // eslint-disable-line

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    api.get(path, { params: { page, size: pageSize, ...params } })
      .then((res) => {
        if (cancelled) return
        const d = unwrap(res) || {}
        setItems(d.content || [])
        setTotalPages(d.totalPages || 0)
        setTotalElements(d.totalElements || 0)
      })
      .catch((e) => { if (!cancelled) toast.error(apiMessage(e)) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [path, page, pageSize, refreshKey, ...deps]) // eslint-disable-line

  return {
    items, setItems, page, setPage, totalPages, totalElements, loading,
    reload: () => setRefreshKey((k) => k + 1),
  }
}
