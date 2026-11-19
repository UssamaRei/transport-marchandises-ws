import { lazy, Suspense } from "react";
import { createBrowserRouter, Link, Navigate, RouterProvider } from "react-router-dom";
import { AuthProvider } from "./auth";
import { AuthBoundary, Shell } from "./Shell";
import { TransportsList, TransportDetail } from "./Transports";
import { CamionsList, CamionDetail } from "./Camions";
import { BonDetail, BonsList } from "./Bons";
import {
  ChargementInfos,
  ChargementsList,
  DetailLayout,
  Marchandises,
  NewChargement,
  PoidsTotal,
  Validation,
} from "./Chargements";
import { Loading } from "./components/live";
import "./live.css";

const Prototype = lazy(() => import("./maquettes/Prototype"));
function Maquettes() {
  return (
    <div className="prototype-view">
      <div className="live-prototype-banner">
        <strong>Maquettes · données de démonstration</strong>
        <span>Ces écrans ne modifient pas la base de données.</span>
        <Link to="/chargements">Ouvrir les chargements réels →</Link>
      </div>
      <Suspense fallback={<Loading />}>
        <Prototype />
      </Suspense>
    </div>
  );
}
function MissingPage() {
  return (
    <div className="live-auth-page">
      <div className="live-auth-card">
        <h1>Cette page est indisponible.</h1>
        <p>Retrouvez vos chargements depuis la liste.</p>
        <a className="btn" href="/chargements">
          Retour aux chargements
        </a>
      </div>
    </div>
  );
}
const router = createBrowserRouter([
  { path: "/maquettes/*", element: <Maquettes />, errorElement: <MissingPage /> },
  {
    element: (
      <AuthProvider>
        <AuthBoundary />
      </AuthProvider>
    ),
    errorElement: <MissingPage />,
    children: [
      {
        element: <Shell />,
        children: [
          { index: true, element: <Navigate to="/chargements" replace /> },
          { path: "/chargements", element: <ChargementsList /> },
          { path: "/camions", element: <CamionsList /> },
          { path: "/camions/:id", element: <CamionDetail /> },
          { path: "/transports", element: <TransportsList /> },
          { path: "/transports/:id", element: <TransportDetail /> },
          { path: "/bons", element: <BonsList /> },
          { path: "/bons/:id", element: <BonDetail /> },
          { path: "/chargements/nouveau", element: <NewChargement /> },
          {
            path: "/chargements/:id",
            element: <DetailLayout />,
            children: [
              { index: true, element: <ChargementInfos /> },
              { path: "marchandises", element: <Marchandises /> },
              { path: "poids", element: <PoidsTotal /> },
              { path: "validation", element: <Validation /> },
            ],
          },
          { path: "*", element: <MissingPage /> },
        ],
      },
    ],
  },
]);
export default function App() {
  return <RouterProvider router={router} />;
}
