/* @flow */

// styles

import styles from './images.css';

// libs
import _ from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component, Element, PropTypes } from 'react';
import { connect } from 'react-redux';
import moment from 'moment';

import { actions } from '../../modules/products/images';

// components
import WaitAnimation from '../common/wait-animation';
import ConfirmationDialog from '../modal/confirmation-dialog';
import Alert from '../alerts/alert';
import { AddButton } from '../common/buttons';
import Accordion from './accordion/accordion';
import ImageCard from './image-card/image-card';
import Upload from './upload/upload';
import EditImage from './edit-image';
import ActionsDropdown from '../bulk-actions/actions-dropdown';
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';

import SortableTiles from './sortable/sortable-tiles';

// types
import type { Album, ImageInfo, FileInfo } from '../../modules/images';

type Props = {
  albums: Array<Album>;
  list: any;
  isLoading: boolean;
  addAlbumInProgress: boolean;
  editAlbumInProgress: boolean;
  uploadImagesInProgress: boolean;
  isImageLoading: (idx: number) => boolean;

  uploadImages: (context: string, albumId: number, files: Array<FileInfo>) => Promise;
  editImage: (album: string, idx: number, info: any) => Promise;
  deleteImage: (context: string, albumId: number, idx: number) => Promise;
  fetchAlbums: (context: string, entityId: number) => Promise;
  addAlbum: (context: string, entityId: number, album: Album) => Promise;
  editAlbum: (context: string, albumId: number, album: Album) => Promise;
  deleteAlbum: (context: string, albumId: number) => Promise;
};

type State = {
  files: Array<FileInfo>;
  newAlbumMode: boolean;
  addImagesMode: boolean;
  editAlbumMode: boolean;
  isEditImageDialogVisible: boolean;
  isDeleteImageDialogVisible: boolean;
  isDeleteAlbumDialogVisible: boolean;
  selectedImage?: SelectedImage;
  selectedAlbumId?: number;
}

type SelectedImage = {
  idx: number;
  album: Album;
  image: FileInfo;
}

class ProductImages extends Component<void, Props, State> {
  static props: Props;

  state: State = {
    files: [],
    newAlbumMode: false,
    addImagesMode: false,
    editAlbumMode: false,
    isEditImageDialogVisible: false,
    isDeleteImageDialogVisible: false,
    isDeleteAlbumDialogVisible: false,
    selectedImage: void 0,
    selectedAlbumId: void 0,
  };

  componentDidMount(): void {
    const { context, productId } = this.props.params;

    this.props.fetchAlbums(context, productId);
  }

  componentWillReceiveProps(nextProps: Props): void {
    if (this.props.uploadImagesInProgress && !nextProps.uploadImagesInProgress) {
      this.setState({
        files: [],
      });
    }
  }

  @autobind
  handleEditImage(selectedImage: SelectedImage): void {
    this.setState({
      selectedImage,
      isEditImageDialogVisible: true,
    });
  }

  @autobind
  handleCancelEditImage(): void {
    this.setState({
      selectedImage: void 0,
      isEditImageDialogVisible: false,
    });
  }

  @autobind
  handleDeleteImage(selectedImage: SelectedImage): void {
    this.setState({
      selectedImage,
      isDeleteImageDialogVisible: true,
    });
  }

  @autobind
  handleCancelRemoveImage(): void {
    this.setState({
      selectedImage: void 0,
      isDeleteImageDialogVisible: false,
    });
  }

  @autobind
  handleAddAlbum() {
    this.setState({
      newAlbumMode: true,
    });
  }

  @autobind
  handleAddImages(selectedAlbumId: number) {
    if (!this.state.addImagesMode) {
      this.setState({
        selectedAlbumId,
        addImagesMode: true,
      });
    } else {
      this.setState({
        selectedAlbumId: void 0,
        addImagesMode: false,
      });
    }
  }

  @autobind
  handleEditAlbum(selectedAlbumId: number): void {
    this.setState({
      selectedAlbumId,
      editAlbumMode: true,
    });
  }

  @autobind
  handleCancelEditAlbum(): void {
    this.setState({
      editAlbumMode: false,
      selectedAlbumId: void 0,
      newAlbumMode: false,
    });
  }

  @autobind
  handleEditAlbumComplete(name: string): void {
    const { context, productId } = this.props.params;

    if (!name.length) {
      return this.handleCancelEditAlbum();
    }

    if (this.state.newAlbumMode) {
      this.props.addAlbum(context, productId, { name }).then(this.handleCancelEditAlbum);
    } else {
      const album = this.album;
      album.name = name;

      this.props.editAlbum(context, album.id, album).then(this.handleCancelEditAlbum);
    }
  }

  @autobind
  handleDeleteAlbum(selectedAlbumId: number): void {
    this.setState({
      selectedAlbumId,
      isDeleteAlbumDialogVisible: true,
    });
  }

  @autobind
  handleCancelDeleteAlbum(): void {
    this.setState({
      selectedAlbumId: void 0,
      isDeleteAlbumDialogVisible: false,
    });
  }

  @autobind
  handleConfirmDeleteAlbum(): void {
    const { selectedAlbumId } = this.state;

    if (selectedAlbumId === void 0) {
      return;
    }

    const { context } = this.props.params;

    this.props.deleteAlbum(context, this.album.id);

    this.setState({
      isDeleteAlbumDialogVisible: false,
      selectedAlbumId: void 0,
    });
  }

  get album(): Album {
    return this.props.albums.find((album:Album) => album.id === this.state.selectedAlbumId);
  }

  get bulkActions(): Array<Array<any>> {
    return [
      ['Preview in a new window', _.noop, '', ''],
      ['Move to a different album', _.noop, 'successfully moved', 'could not be moved'],
      ['Delete', _.noop, 'successfully deleted', 'could not be deleted'],
    ];
  }

  get deleteImageDialog(): ?Element {
    if (!this.state.selectedImage) {
      return;
    }

    return (
      <ConfirmationDialog
        isVisible={this.state.isDeleteImageDialogVisible}
        header='Delete Image'
        body={'Are you sure you want to delete this image?'}
        cancel='Cancel'
        confirm='Yes, Delete'
        cancelAction={this.handleCancelRemoveImage}
        confirmAction={() => {
          if (!this.state.selectedImage) {
            return;
          }
          const { image, album, idx } = this.state.selectedImage;

          this.props.deleteImage(this.props.params.context, album.id, idx);

          this.setState({ isDeleteImageDialogVisible: false });
        }} />
    );
  }

  get editImageDialog(): ?Element {
    if (!this.state.selectedImage) {
      return;
    }

    return (
      <EditImage
        isVisible={this.state.isEditImageDialogVisible}
        title={_.get(this.state, ['selectedImage', 'image', 'title'], '')}
        alt={_.get(this.state, ['selectedImage', 'image', 'alt'], '')}
        onCancel={this.handleCancelEditImage}
        onSave={(form: ImageInfo) => {
          if (!this.state.selectedImage) {
            return;
          }

          const { image, album, idx } = this.state.selectedImage;

          this.props.editImage(this.props.params.context, album.id, idx, form);

          this.setState({ isEditImageDialogVisible: false });
        }} />
    );
  }

  get deleteAlbumDialog(): ?Element {
    if (this.state.selectedAlbumId === void 0) {
      return;
    }

    const body = (
      <div>
        <Alert type="warning">
          Deleting this album will delete <strong>{this.album.images.length} images</strong> from the product.
        </Alert>
        <span>
          Are you sure you want to delete <strong>{this.album.name}</strong> album?
        </span>
      </div>
    );

    return (
      <ConfirmationDialog
        isVisible={this.state.isDeleteAlbumDialogVisible}
        header='Delete Album'
        body={body}
        cancel='Cancel'
        confirm='Yes, Delete'
        cancelAction={this.handleCancelDeleteAlbum}
        confirmAction={this.handleConfirmDeleteAlbum} />
    );
  }

  @autobind
  getImageActions(selectedImage: SelectedImage): Array<any> {
    const actionsHandler = (handler: Function) => {
      return (e: MouseEvent) => {
        e.stopPropagation();
        handler();
      };
    };

    return [
      { name: 'external-link', handler: actionsHandler(() => window.open(selectedImage.image.src)) },
      { name: 'edit', handler: actionsHandler(() => this.handleEditImage(selectedImage)) },
      { name: 'trash', handler: actionsHandler(() => this.handleDeleteImage(selectedImage)) },
    ];
  }

  @autobind
  getAlbumActions(selectedAlbumId: id): Array<any> {
    return [
      { name: 'add', handler: () => this.handleAddImages(selectedAlbumId) },
      { name: 'edit', handler: () => this.handleEditAlbum(selectedAlbumId) },
      { name: 'trash', handler: () => this.handleDeleteAlbum(selectedAlbumId) },
    ];
  }

  @autobind
  onAddFiles(files: Array<FileInfo>): void {
    const f = files.map((file: FileInfo) => ({
      title: file.file.name,
      src: file.src,
      file: file.file,
      loading: true,
    }));

    this.setState(
      { files: [...this.state.files, ...f] },
      () => this.props.uploadImages(this.props.params.context, this.album.id, f)
    );
  }

  get dropzone() {
    const { newAlbumMode, files } = this.state;
    const { addAlbumInProgress } = this.props;

    if (!newAlbumMode && !addAlbumInProgress) {
      return;
    }

    return (
      <div className="fc-grid fc-grid-no-gutter">
        <div className="fc-col-md-1-1">
          <Accordion
            className={classNames({ [styles.addImages] : this.state.addImagesMode })}
            placeholder="New album"
            editMode={newAlbumMode}
            loading={addAlbumInProgress}
            onEditComplete={this.handleEditAlbumComplete}
            onEditCancel={this.handleCancelEditAlbum}
            key="new-album"
          >
            <Upload onDrop={this.onAddFiles}>
              {files.map((image: FileInfo, i) => {
                return <ImageCard src={image.src} key={i}
                                  title={image.title}
                                  loading={image.loading}
                                  secondaryTitle={`Uploaded ${moment().format('MM/DD/YYYY HH: mm')}`} />;
              })}
            </Upload>
          </Accordion>
        </div>
      </div>
    );
  }

  render(): Element {
    if (this.props.isLoading) {
      return <WaitAnimation />;
    }

    return (
      <div>
        {this.editImageDialog}
        {this.deleteImageDialog}
        {this.deleteAlbumDialog}
        <div className="fc-table__header">
          <ActionsDropdown actions={this.bulkActions}
                           disabled={false}
                           allChecked={false}
                           toggledIds={[]}
                           total={0} />
          <AddButton onClick={this.handleAddAlbum}>Album</AddButton>
        </div>
        {/**
         <div className="fc-grid fc-grid-no-gutter">
         <div className="fc-col-md-1-1">
         {this.contentBox}
         </div>
         </div>
         */}
            {this.dropzone}
        {_.values(_.map(this.props.albums, this.renderAlbum))}
      </div>
    );
  }

  @autobind
  renderAlbum(album: Album): Element {
    const { selectedAlbumId, addImagesMode, editAlbumMode } = this.state;
    const { editAlbumInProgress } = this.props;

    const activeAlbum = selectedAlbumId === album.id;
    const addMode = activeAlbum && addImagesMode;
    const editTitleMode = activeAlbum && editAlbumMode;
    const loading = activeAlbum && editAlbumInProgress;

    const files = album ? [...album.images, ...this.state.files] : this.state.files;

    let accordionContent;

    if (addMode) {
      accordionContent = (
        <Upload onDrop={this.onAddFiles}>
          {files.map((image: FileInfo, i) => {
            return <ImageCard src={image.src}
                              key={i}
                              title={image.title}
                              loading={image.loading}
                              secondaryTitle={`Uploaded ${moment().format('MM/DD/YYYY HH: mm')}`} />;
          })}
        </Upload>
      );
    } else {
      accordionContent = (
        <SortableTiles itemWidth={298} itemHeight={372} gutter={10}>
          {album.images.map((image: FileInfo, idx: number) => {
            return (
              <ImageCard
                src={`${image.src}`}
                title={image.title}
                secondaryTitle={`Uploaded ${image.uploadedAt || moment().format('MM/DD/YYYY HH: mm')}`}
                actions={this.getImageActions({image, album, idx})}
                loading={image.loading}
                key={image.id}
              />
            );
          })}
        </SortableTiles>
      );
    }

    return (
      <div className="fc-grid fc-grid-no-gutter" key={album.id}>
        <div className="fc-col-md-1-1">
          <Accordion
            className={classNames({ [styles.addImages] : activeAlbum && this.state.addImagesMode })}
            title={album.name}
            titleWrapper={(title: string) => this.renderTitle(title, album.images.length)}
            placeholder="Album Name"
            open={addMode}
            loading={loading}
            editMode={editTitleMode}
            onEditComplete={this.handleEditAlbumComplete}
            onEditCancel={this.handleCancelEditAlbum}
            actions={this.getAlbumActions(album.id)}
          >
            {accordionContent}
          </Accordion>
        </div>
      </div>
    );
  }

  @autobind
  renderTitle(title, count): Element {
    return (
      <span>
        <span className="fc-product-details__images-title-text">{title}</span>
        <span className="fc-product-details__images-title-count">{count}</span>
      </span>
    );
  }
}

const mapState = (state) => ({
  list: _.get(state, ['products', 'images', 'list'], []),
  albums: _.get(state, ['products', 'images', 'albums'], []),
  isLoading: _.get(state, ['asyncActions', 'productsFetchAlbums', 'inProgress'], true),
  addAlbumInProgress: _.get(state, ['asyncActions', 'productsAddAlbum', 'inProgress'], false),
  editAlbumInProgress: _.get(state, ['asyncActions', 'productsEditAlbum', 'inProgress'], false),
  uploadImagesInProgress: _.get(state, ['asyncActions', 'productsUploadImages', 'inProgress'], false),
});

export default connect(mapState, actions)(ProductImages);

function guid(): string {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }

  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
}
