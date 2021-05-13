import React, { Component } from 'react';
import { withStyles } from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import DialogActions from '@material-ui/core/DialogActions';
import SetupIcon from '@material-ui/icons/Settings';
import CancelIcon from '@material-ui/icons/Close';

import { Button } from 'react-admin';
import SetupCube from '../setup/SetupCube';

const styles = theme => ({
	btnPadding: {
        paddingRight: theme.spacing(1)
    },
    title: {
        display: 'inline-flex',
        alignItems: 'center',
    },
    content: {
		padding: 0
	}
});

class SetupButton extends Component {
    constructor(props) {
        super(props);
        this.state = {
            showDialog: false
        }
    }

    handleOpen = () => {
        this.setState({showDialog: true});
    };

    handleClose = () => {
        this.setState({showDialog: false});
    };

    render() {
        const { classes, record } = this.props;
		const { showDialog } = this.state;

        return (
            record
            ?
            <span>
                <Button label='Setup' onClick={this.handleOpen} >
                    <SetupIcon />
                </Button>
				<Dialog
					fullWidth
					maxWidth='sm'								
                    open={showDialog}
                    scroll={'paper'}
					onClose={this.handleClose}
					disableBackdropClick={true}
					aria-labelledby='setup-dialog-title'
				>
					<DialogTitle id='setup-dialog-title'>
						<span className={classes.title}>
                            <SetupIcon className={classes.btnPadding} />
                            {record && `${record.name}`} - Setup Connections
						</span>
					</DialogTitle>
					
					<DialogContent className={classes.content}>
                        <SetupCube cubeID={record.id} deviceType={record.deviceType} />
					</DialogContent>

					<DialogActions>
						<Button label='Close' onClick={this.handleClose} >
 				           <CancelIcon />
        				</Button>
                    </DialogActions>
				</Dialog>
            </span>
            :
            null
        );
    }
}

export default withStyles(styles)(SetupButton);
