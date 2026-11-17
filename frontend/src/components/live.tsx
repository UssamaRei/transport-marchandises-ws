import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useBeforeUnload, useBlocker } from "react-router-dom";
import { ApiError } from "../api";
import { Button, Icon } from "./ui";

const ToastContext = createContext<(text: string) => void>(() => undefined);
export function ToastProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState("");
  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(() => setMessage(""), 5500);
    return () => window.clearTimeout(timer);
  }, [message]);
  return (
    <ToastContext.Provider value={setMessage}>
      {children}
      {message && (
        <div className="toast live-toast" role="status">
          <Icon name="check" />
          <span>{message}</span>
          <button type="button" aria-label="Fermer la notification" onClick={() => setMessage("")}>
            <Icon name="close" size={16} />
          </button>
        </div>
      )}
    </ToastContext.Provider>
  );
}
export const useToast = () => useContext(ToastContext);
export function Loading({ text = "Chargement en cours…" }: { text?: string }) {
  return (
    <div className="live-loading" role="status">
      <span className="live-spinner" />
      {text}
    </div>
  );
}
export function ErrorBanner({ error, retry }: { error: ApiError | null; retry?: () => void }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (error) ref.current?.focus();
  }, [error]);
  if (!error) return null;
  return (
    <div className="live-error" role="alert" ref={ref} tabIndex={-1}>
      <Icon name="alert" size={20} />
      <div>
        <strong>{error.message}</strong>
        {Object.keys(error.fields).length > 0 && (
          <ul>
            {[...new Set(Object.values(error.fields))].map((value) => (
              <li key={value}>{value}</li>
            ))}
          </ul>
        )}
      </div>
      {retry && (
        <Button secondary onClick={retry}>
          Réessayer
        </Button>
      )}
    </div>
  );
}
export function Modal({
  title,
  children,
  close,
  busy = false,
}: {
  title: string;
  children: ReactNode;
  close: () => void;
  busy?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    dialog?.showModal();
    return () => dialog?.close();
  }, []);
  return (
    <dialog
      className="live-dialog"
      ref={ref}
      aria-labelledby="live-dialog-title"
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) close();
      }}
    >
      <div className="live-dialog-heading">
        <h2 id="live-dialog-title">{title}</h2>
        <button
          type="button"
          className="icon-button"
          aria-label="Fermer"
          disabled={busy}
          onClick={close}
        >
          <Icon name="close" />
        </button>
      </div>
      {children}
    </dialog>
  );
}
export function useDirtyGuard(dirty: boolean) {
  const bypass = useRef(false);
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      dirty && !bypass.current && currentLocation.pathname !== nextLocation.pathname,
  );
  useEffect(() => {
    if (blocker.state !== "blocked") return;
    if (window.confirm("Quitter cette page sans enregistrer vos modifications ?"))
      blocker.proceed();
    else blocker.reset();
  }, [blocker]);
  useBeforeUnload(
    useCallback(
      (event: BeforeUnloadEvent) => {
        if (dirty && !bypass.current) {
          event.preventDefault();
          event.returnValue = "";
        }
      },
      [dirty],
    ),
  );
  return () => {
    bypass.current = true;
  };
}
