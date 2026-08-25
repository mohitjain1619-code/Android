'use client';
import { useState, useEffect, useRef } from 'react';
import { X, Trash2 } from 'lucide-react';
import { uploadTextStory, uploadMediaStory, deleteStory } from '../lib/api';
import styles from './StoryModals.module.css';

// Gradient mappings for Text Stories
const GRADIENTS = {
  'bg_community_hot_gradient': 'linear-gradient(135deg, #E040FB, #00E5FF)',
  'bg_neon_amber_button': 'linear-gradient(135deg, #FFD700, #FFAA00)',
  'cyan': 'linear-gradient(135deg, #00E5FF, #00838F)',
  'dark': 'linear-gradient(135deg, #1A1A1A, #0A0A0A)'
};

// ============================================
// STORY UPLOAD MODAL COMPONENT (Admin-Only)
// ============================================
export function StoryUploadModal({ onClose, onUploadSuccess }) {
  const [type, setType] = useState('TEXT'); // TEXT, IMAGE, VIDEO
  const [textContent, setTextContent] = useState('');
  const [textColor, setTextColor] = useState('#FFFFFF');
  const [bgGradient, setBgGradient] = useState('bg_community_hot_gradient');
  
  const [mediaFile, setMediaFile] = useState(null);
  const [mediaPreview, setMediaPreview] = useState(null);
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fileInputRef = useRef(null);

  // Switch cleanups
  const handleTypeChange = (e) => {
    setType(e.target.value);
    setMediaFile(null);
    setMediaPreview(null);
    setTextContent('');
    setError('');
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Check size limit (15MB)
    if (file.size > 15 * 1024 * 1024) {
      setError('File size exceeds the 15MB limit.');
      return;
    }

    setMediaFile(file);
    setError('');

    const reader = new FileReader();
    reader.onloadend = () => {
      setMediaPreview(reader.result);
    };
    reader.readAsDataURL(file);
  };

  const handlePublish = async () => {
    if (type === 'TEXT' && !textContent.trim()) {
      setError('Please write some text content.');
      return;
    }
    if ((type === 'IMAGE' || type === 'VIDEO') && !mediaFile) {
      setError('Please select a media file.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      if (type === 'TEXT') {
        const res = await uploadTextStory({
          type: 'TEXT',
          textContent,
          textColor,
          bgGradient
        });
        if (res.ok) {
          onUploadSuccess();
          onClose();
        } else {
          setError(res.error || 'Failed to publish text story.');
        }
      } else {
        const formData = new FormData();
        formData.append('media', mediaFile);
        formData.append('type', type);
        formData.append('textContent', textContent);
        formData.append('textColor', '#FFFFFF');
        formData.append('bgGradient', '');

        const res = await uploadMediaStory(formData);
        if (res.ok) {
          onUploadSuccess();
          onClose();
        } else {
          setError(res.error || 'Failed to publish media story.');
        }
      }
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.error || 'Network error uploading story.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.overlay}>
      <div className={styles.modalContent}>
        <button className={styles.closeButton} onClick={onClose}>
          <X size={20} />
        </button>

        <h3 className={styles.title}>Publish A Story</h3>

        {error && (
          <div style={{ color: '#FF2D55', fontSize: '13px', marginBottom: '12px', textAlign: 'center' }}>
            ⚠️ {error}
          </div>
        )}

        <div className={styles.formGroup}>
          <label className={styles.label}>STORY TYPE</label>
          <select className={styles.select} value={type} onChange={handleTypeChange}>
            <option value="TEXT">Text Only</option>
            <option value="IMAGE">Image Media</option>
            <option value="VIDEO">Video Media</option>
          </select>
        </div>

        {/* Text Story Custom Editor Preview */}
        {type === 'TEXT' && (
          <>
            <div className={styles.canvasPreview} style={{ background: GRADIENTS[bgGradient] }}>
              <div className={styles.canvasText} style={{ color: textColor }}>
                {textContent || 'Preview Story Text'}
              </div>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>STORY TEXT</label>
              <textarea
                className={styles.textarea}
                placeholder="What's on your mind?..."
                value={textContent}
                onChange={(e) => {
                  setTextContent(e.target.value.slice(0, 200));
                  setError('');
                }}
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>CANVAS BACKGROUND</label>
              <div className={styles.themeSelector}>
                {Object.keys(GRADIENTS).map((g) => (
                  <div
                    key={g}
                    className={`${styles.themeCircle} ${bgGradient === g ? styles.themeCircleActive : ''}`}
                    style={{ background: GRADIENTS[g] }}
                    onClick={() => setBgGradient(g)}
                  />
                ))}
              </div>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>TEXT COLOR</label>
              <input
                className={styles.input}
                type="color"
                value={textColor}
                onChange={(e) => setTextColor(e.target.value)}
                style={{ height: '36px', padding: '2px', cursor: 'pointer' }}
              />
            </div>
          </>
        )}

        {/* Image/Video Upload Custom Editor Preview */}
        {(type === 'IMAGE' || type === 'VIDEO') && (
          <>
            <div className={styles.canvasPreview}>
              {mediaPreview ? (
                type === 'IMAGE' ? (
                  <img className={styles.viewerMedia} src={mediaPreview} alt="Preview" />
                ) : (
                  <video className={styles.viewerMedia} src={mediaPreview} autoPlay muted loop />
                )
              ) : (
                <div style={{ color: '#8e8e93', fontSize: '14px' }}>No Media Selected</div>
              )}
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>SELECT ATTACHMENT</label>
              <input
                type="file"
                ref={fileInputRef}
                style={{ display: 'none' }}
                accept={type === 'IMAGE' ? 'image/*' : 'video/*'}
                onChange={handleFileChange}
              />
              <button
                className={styles.publishBtn}
                style={{ background: 'rgba(255, 255, 255, 0.1)', color: '#fff', border: '1px dashed rgba(255,255,255,0.3)', marginBottom: '16px' }}
                onClick={() => fileInputRef.current.click()}
              >
                {mediaFile ? `Change File: ${mediaFile.name.slice(0, 20)}...` : 'Select from Files'}
              </button>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>OVERLAY CAPTION TEXT (OPTIONAL)</label>
              <input
                className={styles.input}
                type="text"
                placeholder="Add caption overlay..."
                value={textContent}
                onChange={(e) => {
                  setTextContent(e.target.value);
                  setError('');
                }}
              />
            </div>
          </>
        )}

        <button className={styles.publishBtn} onClick={handlePublish} disabled={loading}>
          {loading ? 'Publishing...' : 'Share to Stories'}
        </button>
      </div>
    </div>
  );
}

// ============================================
// STORIES VIEWER MODAL COMPONENT (Instagram-style)
// ============================================
export function StoryViewerModal({ userStories, loggedInEmail, onClose, onDeleteSuccess }) {
  const stories = userStories.stories || [];
  const [currentIndex, setCurrentIndex] = useState(0);
  const [progress, setProgress] = useState(0);
  const [paused, setPaused] = useState(false);
  const [videoDuration, setVideoDuration] = useState(5000);
  const [videoLoaded, setVideoLoaded] = useState(false);

  const videoRef = useRef(null);

  const activeStory = stories[currentIndex];
  const isAdmin = loggedInEmail === 'mohitjain1619@gmail.com';

  const storyDuration = activeStory?.type === 'VIDEO' ? videoDuration : 5000;

  // Reset progress and states on story index change
  useEffect(() => {
    setProgress(0);
    setVideoLoaded(false);
    setPaused(false);
  }, [currentIndex]);

  // Timer loop for progress animation
  useEffect(() => {
    if (!activeStory) return;
    if (paused) return;

    // For video stories, pause progress bar until video metadata loads
    if (activeStory.type === 'VIDEO' && !videoLoaded) return;

    const intervalMs = 50;
    const increment = (100 * intervalMs) / storyDuration;

    const timer = setInterval(() => {
      setProgress((prev) => {
        if (prev + increment >= 100) {
          clearInterval(timer);
          handleNextStory();
          return 100;
        }
        return prev + increment;
      });
    }, intervalMs);

    return () => clearInterval(timer);
  }, [currentIndex, paused, videoLoaded, videoDuration]);

  const handleNextStory = () => {
    if (currentIndex + 1 < stories.length) {
      setCurrentIndex(currentIndex + 1);
    } else {
      onClose(); // End of stories
    }
  };

  const handlePrevStory = () => {
    if (currentIndex - 1 >= 0) {
      setCurrentIndex(currentIndex - 1);
    } else {
      setCurrentIndex(0);
    }
  };

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this story?')) return;

    try {
      const res = await deleteStory(activeStory.id);
      if (res.ok) {
        onDeleteSuccess();
        // Remove locally from list
        stories.splice(currentIndex, 1);
        if (stories.length === 0) {
          onClose();
        } else {
          if (currentIndex >= stories.length) {
            setCurrentIndex(stories.length - 1);
          } else {
            setCurrentIndex(currentIndex);
          }
        }
      } else {
        alert('Failed to delete story');
      }
    } catch (err) {
      console.error(err);
      alert('Error deleting story');
    }
  };

  const handleVideoMetadata = () => {
    if (videoRef.current) {
      const durationMs = videoRef.current.duration * 1000;
      setVideoDuration(durationMs > 0 ? durationMs : 5000);
      setVideoLoaded(true);
    }
  };

  const apiHost = process.env.NEXT_PUBLIC_API_URL ? process.env.NEXT_PUBLIC_API_URL.replace('/api', '') : '';

  return (
    <div className={styles.viewerContainer}>
      <div className={styles.viewerCanvas}>
        
        {/* Left & Right click navigations */}
        <div className={styles.navClickLeft} onClick={handlePrevStory} />
        <div className={styles.navClickRight} onClick={handleNextStory} />

        {/* Stories Renderers */}
        {activeStory && (
          <>
            {activeStory.type === 'TEXT' && (
              <div
                className={styles.viewerTextOnly}
                style={{ background: GRADIENTS[activeStory.bgGradient] || GRADIENTS['bg_community_hot_gradient'], color: activeStory.textColor }}
              >
                {activeStory.textContent}
              </div>
            )}

            {activeStory.type === 'IMAGE' && (
              <img
                className={styles.viewerMedia}
                src={`${apiHost}${activeStory.mediaUrl}`}
                alt="Story content"
              />
            )}

            {activeStory.type === 'VIDEO' && (
              <video
                ref={videoRef}
                className={styles.viewerMedia}
                src={`${apiHost}${activeStory.mediaUrl}`}
                autoPlay
                muted
                playsInline
                onLoadedMetadata={handleVideoMetadata}
              />
            )}

            {/* Caption Text Overlay for media stories */}
            {activeStory.type !== 'TEXT' && activeStory.textContent && (
              <div className={styles.viewerOverlayText}>{activeStory.textContent}</div>
            )}
          </>
        )}

        {/* Top Indicators and Details Header */}
        <div className={styles.viewerHeader}>
          {/* Segmented indicators row */}
          <div className={styles.progressBarGroup}>
            {stories.map((_, i) => {
              let width = '0%';
              if (i < currentIndex) width = '100%';
              else if (i === currentIndex) width = `${progress}%`;

              return (
                <div key={i} className={styles.progressBarContainer}>
                  <div className={styles.progressBarFill} style={{ width }} />
                </div>
              );
            })}
          </div>

          <div className={styles.viewerUserRow}>
            <div className={styles.viewerUserMeta}>
              <img
                className={styles.viewerAvatar}
                src={userStories.userAvatar ? `/avatars/${userStories.userAvatar}.png` : '/avatars/av1.png'}
                alt={userStories.userName}
              />
              <span className={styles.viewerName}>{userStories.userName}</span>
            </div>

            <button className={styles.closeButton} style={{ top: 'auto', right: 'auto', position: 'relative' }} onClick={onClose}>
              <X size={24} />
            </button>
          </div>
        </div>

        {/* Admin Delete Action button overlay */}
        {isAdmin && (
          <button className={styles.deleteStoryBtn} onClick={handleDelete}>
            <Trash2 size={20} />
          </button>
        )}
      </div>
    </div>
  );
}
