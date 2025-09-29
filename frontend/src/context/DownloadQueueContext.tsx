import { createContext, useState, useContext } from 'react';
import type { ReactNode } from 'react';
import type { Track } from '../types/Track'; // We will create this type definition next

interface DownloadQueueContextType {
    queue: Track[];
    addToQueue: (track: Track) => void;
    removeFromQueue: (trackId: string) => void;
    clearQueue: () => void;
    isInQueue: (trackId: string) => boolean;
}

const DownloadQueueContext = createContext<DownloadQueueContextType | undefined>(undefined);

export const DownloadQueueProvider = ({ children }: { children: ReactNode }) => {
    const [queue, setQueue] = useState<Track[]>([]);

    const addToQueue = (track: Track) => {
        setQueue((prevQueue) => {
            if (prevQueue.find(t => t.id === track.id)) {
                return prevQueue; // Avoid duplicates
            }
            return [...prevQueue, track];
        });
    };

    const removeFromQueue = (trackId: string) => {
        setQueue((prevQueue) => prevQueue.filter(t => t.id !== trackId));
    };

    const clearQueue = () => {
        setQueue([]);
    };

    const isInQueue = (trackId: string) => {
        return queue.some(t => t.id === trackId);
    };

    return (
        <DownloadQueueContext.Provider value={{ queue, addToQueue, removeFromQueue, clearQueue, isInQueue }}>
            {children}
        </DownloadQueueContext.Provider>
    );
};

export const useDownloadQueue = () => {
    const context = useContext(DownloadQueueContext);
    if (context === undefined) {
        throw new Error('useDownloadQueue must be used within a DownloadQueueProvider');
    }
    return context;
};
