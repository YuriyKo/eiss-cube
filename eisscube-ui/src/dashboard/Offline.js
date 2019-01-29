import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import Avatar from '@material-ui/core/Avatar';
import OfflineIcon from '@material-ui/icons/ThumbDown';
import ListIcon from '@material-ui/icons/List';
import { red } from '@material-ui/core/colors';

import { Button } from 'react-admin';

const styles = theme => ({
    card: { 
        borderLeft: `solid 4px ${red[500]}`, 
        flex: 1, 
        marginBottom: theme.spacing.unit 
    },
    avatar: {
        color: theme.palette.common.white,
        backgroundColor: red[500]
    }
});

export default withStyles(styles)(
    ({classes, value}) => (
        <Card className={classes.card} >
            <CardHeader
                avatar={
                    <Avatar className={classes.avatar}>
                        <OfflineIcon />
                    </Avatar>
                }
                title={`${value} EISS™Cube(s)`}
                subheader={<span style={{ color: red[500] }}>OFFLINE</span>}
                action={
                    <Button 
                        label='List'
                        href={'#/cubes?filter={"online":false}&page=1&perPage=10&sort=deviceID&order=DESC'} >
                        <ListIcon />
                    </Button>
                }
            />
        </Card>
    )
);
